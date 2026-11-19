package com.banking.ledger.reconciliation.controller;

import com.banking.ledger.reconciliation.dto.AuthDtos.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() throws Exception {
        TokenResponse tokens = login("admin", "admin123");

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.roles()).contains("ADMIN");
        assertThat(tokens.expiresIn()).isPositive();

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isOk());
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("admin", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void auditorCanReadButNotWrite() throws Exception {
        TokenResponse auditor = login("auditor", "auditor123");

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, bearer(auditor)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(auditor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(account("AUDITOR-DENIED-" + shortId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanCloseOrDeleteAccounts() throws Exception {
        long accountId = createAccount(login("admin", "admin123"));
        TokenResponse accountant = login("accountant", "accountant123");

        mockMvc.perform(patch("/api/accounts/{id}/status", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accountant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/accounts/{id}/status", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accountant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accountant)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/accounts/{id}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin", "admin123"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/accounts/{id}", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin", "admin123"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void refreshTokenRotatesAndOldTokenIsRejected() throws Exception {
        TokenResponse original = login("admin", "admin123");

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(original.refreshToken())))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse rotated = objectMapper.readValue(
                refreshed.getResponse().getContentAsString(), TokenResponse.class);
        assertThat(rotated.refreshToken()).isNotEqualTo(original.refreshToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(original.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        TokenResponse tokens = login("admin", "admin123");

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(tokens.refreshToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(tokens.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenCannotBeUsedAsAnAccessToken() throws Exception {
        TokenResponse tokens = login("admin", "admin123");

        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    private TokenResponse login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private long createAccount(TokenResponse tokens) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(account("AUTH-" + shortId())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private String account(String accountNumber) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "accountNumber", accountNumber,
                "accountName", "Test Account",
                "bankName", "Test Bank",
                "currency", "USD",
                "accountType", "CHECKING",
                "openingBalance", 1000.00));
    }

    private String credentials(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    private String refreshBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
    }

    private static String bearer(TokenResponse tokens) {
        return "Bearer " + tokens.accessToken();
    }

    /** Account numbers are limited to 34 characters. */
    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}