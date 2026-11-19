package com.banking.ledger.reconciliation.controller;

import com.banking.ledger.reconciliation.domain.AuditLog;
import com.banking.ledger.reconciliation.domain.BankAccount;
import com.banking.ledger.reconciliation.domain.enums.AuditAction;
import com.banking.ledger.reconciliation.dto.AuthDtos.TokenResponse;
import com.banking.ledger.reconciliation.repository.AuditLogRepository;
import com.banking.ledger.reconciliation.repository.BankAccountRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditTrailTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Test
    void mutationsRecordActorAndEntityAuthorship() throws Exception {
        TokenResponse admin = login("admin", "admin123");
        String accountNumber = "AUDIT-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(account(accountNumber)))
                .andExpect(status().isCreated());

        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(account.getCreatedBy()).isEqualTo("admin");

        AuditLog log = auditLogRepository.findAll().stream()
                .filter(entry -> entry.getAction() == AuditAction.ACCOUNT_CREATED)
                .filter(entry -> String.valueOf(account.getId()).equals(entry.getEntityId()))
                .findFirst()
                .orElseThrow();

        assertThat(log.getActor()).isEqualTo("admin");
        assertThat(log.getDetails()).contains(accountNumber);
        assertThat(log.getOccurredAt()).isNotNull();
    }

    @Test
    void onlyAdminAndAuditorCanReadTheAuditTrail() throws Exception {
        mockMvc.perform(get("/api/audit")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("auditor", "auditor123"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin", "admin123"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("accountant", "accountant123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void failedLoginsAreAudited() throws Exception {
        String username = "missing-" + UUID.randomUUID();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", "nope"))))
                .andExpect(status().isUnauthorized());

        assertThat(auditLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getAction()).isEqualTo(AuditAction.LOGIN_FAILED);
                    assertThat(log.getActor()).isEqualTo(username);
                });
    }

    private TokenResponse login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private String account(String accountNumber) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "accountNumber", accountNumber,
                "accountName", "Audit Account",
                "bankName", "Test Bank",
                "currency", "USD",
                "accountType", "CHECKING",
                "openingBalance", 100.00));
    }

    private static String bearer(TokenResponse tokens) {
        return "Bearer " + tokens.accessToken();
    }
}