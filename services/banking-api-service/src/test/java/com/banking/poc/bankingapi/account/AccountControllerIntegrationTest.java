package com.banking.poc.bankingapi.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerIntegrationTest {

    private static final String TEST_JWT = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ1c2VyLTEyMyJ9.";

    @LocalServerPort
    private int port;

    @Test
    void returnsAccountDetailsForKnownAccount() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/accounts/A-1001"))
                .header("Authorization", "Bearer " + TEST_JWT)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"accountId\":\"A-1001\""));
        assertTrue(response.body().contains("\"customerId\":\"C-1001\""));
        assertTrue(response.body().contains("\"currency\":\"GBP\""));
    }
}
