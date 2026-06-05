package com.banking.poc.identitybootstrap.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DemoUserController.class, properties = "demo.bootstrap.secret=test-secret")
class DemoUserControllerTest {

    private static final String DEMO_BOOTSTRAP_SECRET_HEADER = "X-Demo-Bootstrap-Secret";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoUserService demoUserService;

    @Test
    void createsDemoUser() throws Exception {
        DemoUserRequest request = new DemoUserRequest(
                "demo-user",
                "Password123!",
                "customer",
                "C-1001",
                List.of("A-1001", "A-1002"));
        DemoUserCreatedResponse response = new DemoUserCreatedResponse("demo-user", "customer", "created");
        given(demoUserService.createDemoUser(any())).willReturn(response);

        mockMvc.perform(post("/demo/users")
                        .header(DEMO_BOOTSTRAP_SECRET_HEADER, "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo-user",
                                  "password": "Password123!",
                                  "role": "customer",
                                  "customerId": "C-1001",
                                  "accountIds": ["A-1001", "A-1002"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"username\":\"demo-user\"")))
                .andExpect(content().string(containsString("\"role\":\"customer\"")))
                .andExpect(content().string(containsString("\"status\":\"created\"")));

        then(demoUserService).should().createDemoUser(request);
    }

    @Test
    void rejectsCreateDemoUserWithoutBootstrapSecret() throws Exception {
        mockMvc.perform(post("/demo/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo-user",
                                  "password": "Password123!",
                                  "role": "customer",
                                  "customerId": "C-1001",
                                  "accountIds": ["A-1001", "A-1002"]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
