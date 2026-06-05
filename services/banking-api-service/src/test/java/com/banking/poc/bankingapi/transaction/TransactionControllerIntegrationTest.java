package com.banking.poc.bankingapi.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerIntegrationTest {

    private static final String TEST_JWT = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTEyMyJ9.";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Test
    void returnsTransactionsForKnownAccount() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-1001/transactions"))
                .header("Authorization", "Bearer " + TEST_JWT)
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

    private record TransactionResponse(String accountId, BigDecimal amount) {
    }
}
