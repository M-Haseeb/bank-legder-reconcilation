package com.banking.ledger.reconciliation.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.net.URI;

/**
 * Writes RFC 9457 problem responses from places that sit outside MVC, such as
 * servlet filters and Spring Security handlers.
 */
public final class ProblemDetailWriter {

    private ProblemDetailWriter() {
    }

    public static void write(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String code,
            String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:problem:" + code.toLowerCase()));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}