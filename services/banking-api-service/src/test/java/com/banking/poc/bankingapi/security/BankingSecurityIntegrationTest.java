package com.banking.poc.bankingapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankingSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsUnauthenticatedAccountRequest() throws Exception {
        mockMvc.perform(get("/api/accounts/A-1001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsUnauthenticatedHealthRequest() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"UP\"")));
    }

    @Test
    void allowsAuthenticatedAccountRequest() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(customerJwt("C-1001", List.of("A-1001")));

        mockMvc.perform(get("/api/accounts/A-1001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"accountId\":\"A-1001\"")));

        then(jwtDecoder).should().decode("test-token");
    }

    @Test
    void rejectsAuthenticatedCustomerRequestForForeignAccount() throws Exception {
        given(jwtDecoder.decode("test-token")).willReturn(customerJwt("C-1001", List.of("A-1001")));

        mockMvc.perform(get("/api/accounts/A-2001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isForbidden());
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
}
