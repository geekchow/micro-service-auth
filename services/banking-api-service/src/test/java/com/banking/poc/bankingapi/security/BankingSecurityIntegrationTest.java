package com.banking.poc.bankingapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

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
    private JwtAuthenticationDecoder jwtAuthenticationDecoder;

    @Test
    void rejectsUnauthenticatedAccountRequest() throws Exception {
        mockMvc.perform(get("/api/accounts/A-1001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsAuthenticatedAccountRequest() throws Exception {
        given(jwtAuthenticationDecoder.decode("test-token")).willReturn(new JwtClaims("user-123"));

        mockMvc.perform(get("/api/accounts/A-1001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"accountId\":\"A-1001\"")));

        then(jwtAuthenticationDecoder).should().decode("test-token");
    }
}
