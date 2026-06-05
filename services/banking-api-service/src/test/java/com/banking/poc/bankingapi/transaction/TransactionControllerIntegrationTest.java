package com.banking.poc.bankingapi.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private JwtDecoder jwtDecoder;

    @LocalServerPort
    private int port;

    @Test
    void returnsTransactionsForKnownAccount() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(customerJwt("C-1001", List.of("A-1001")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-1001/transactions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        TransactionResponse[] transactions = objectMapper.readValue(response.body(), TransactionResponse[].class);

        assertEquals(200, response.statusCode());
        assertEquals(1, transactions.length);
        assertEquals("A-1001", transactions[0].accountId());
        assertEquals(new BigDecimal("-12.35"), transactions[0].amount());
    }

    @Test
    void usesBigDecimalForTransactionAmount() {
        assertEquals(BigDecimal.class, TransactionDto.class.getRecordComponents()[1].getType());
    }

    @Test
    void rejectsDirectTransactionAccessToForeignAccountWithValidCustomerToken() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(customerJwt("C-1001", List.of("A-1001")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-2001/transactions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    @Test
    void returnsNotFoundForUnknownAccountTransactions() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(opsAdminJwt());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-9999/transactions"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void rejectsUnclaimedUnknownAccountTransactionsBeforeRepositoryLookup() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(customerJwt("C-1001", List.of("A-1001")));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-9999/transactions"))
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

    private record TransactionResponse(String accountId, BigDecimal amount) {
    }
}
