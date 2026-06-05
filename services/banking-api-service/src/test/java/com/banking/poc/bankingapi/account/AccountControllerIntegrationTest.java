package com.banking.poc.bankingapi.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerIntegrationTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @LocalServerPort
    private int port;

    @Test
    void returnsAccountDetailsForKnownAccount() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(customerJwt("C-1001", List.of("A-1001")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-1001"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"accountId\":\"A-1001\""));
        assertTrue(response.body().contains("\"customerId\":\"C-1001\""));
        assertTrue(response.body().contains("\"currency\":\"GBP\""));
    }

    @Test
    void returnsSecondDemoAccountDetails() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(opsAdminJwt());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-2001"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"accountId\":\"A-2001\""));
        assertTrue(response.body().contains("\"customerId\":\"C-2001\""));
    }

    @Test
    void rejectsDirectAccessToForeignAccountWithValidCustomerToken() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(customerJwt("C-1001", List.of("A-1001")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-2001"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    @Test
    void rejectsUnclaimedUnknownAccountBeforeRepositoryLookup() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(customerJwt("C-1001", List.of("A-1001")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-9999"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    private Jwt customerJwt(String customerId, List<String> accountIds) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "user-123")
                .claim("customer_id", customerId)
                .claim("account_ids", accountIds)
                .claim("realm_access", java.util.Map.of("roles", List.of("customer")))
                .build();
    }

    private Jwt opsAdminJwt() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "ops-admin")
                .claim("customer_id", "C-9999")
                .claim("account_ids", List.of("A-1001", "A-2001"))
                .claim("realm_access", java.util.Map.of("roles", List.of("ops-admin")))
                .build();
    }
}
