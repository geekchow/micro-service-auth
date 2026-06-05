package com.banking.poc.bankingapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void jwtValidatorAcceptsExpectedIssuerAndAudience() {
        OAuth2TokenValidatorResult result = SecurityConfig.jwtValidator(
                "http://keycloak:8080/realms/banking-poc",
                "mobile-banking-app")
                .validate(testJwt("http://keycloak:8080/realms/banking-poc", List.of("mobile-banking-app")));

        assertFalse(result.hasErrors());
    }

    @Test
    void jwtValidatorRejectsUnexpectedIssuer() {
        OAuth2TokenValidatorResult result = SecurityConfig.jwtValidator(
                "http://keycloak:8080/realms/banking-poc",
                "mobile-banking-app")
                .validate(testJwt("http://keycloak:8080/realms/other", List.of("mobile-banking-app")));

        assertTrue(result.hasErrors());
    }

    @Test
    void jwtValidatorRejectsMissingAudience() {
        OAuth2TokenValidatorResult result = SecurityConfig.jwtValidator(
                "http://keycloak:8080/realms/banking-poc",
                "mobile-banking-app")
                .validate(testJwt("http://keycloak:8080/realms/banking-poc", List.of("account")));

        assertTrue(result.hasErrors());
    }

    private Jwt testJwt(String issuer, List<String> audience) {
        Instant issuedAt = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audience)
                .subject("user-123")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .build();
    }
}
