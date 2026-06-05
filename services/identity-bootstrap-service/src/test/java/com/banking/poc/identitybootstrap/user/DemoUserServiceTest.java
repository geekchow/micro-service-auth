package com.banking.poc.identitybootstrap.user;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

class DemoUserServiceTest {

    @Test
    void delegatesDemoUserCreationToProvisioner() {
        KeycloakUserProvisioner provisioner = mock(KeycloakUserProvisioner.class);
        DemoUserService service = new DemoUserService(provisioner);
        DemoUserRequest request = new DemoUserRequest(
                "demo-user",
                "Password123!",
                "customer",
                "C-1001",
                List.of("A-1001", "A-1002"));
        DemoUserCreatedResponse response = new DemoUserCreatedResponse("demo-user", "customer", "created");

        given(provisioner.createDemoUser(request)).willReturn(response);

        assertEquals(response, service.createDemoUser(request));
        then(provisioner).should().createDemoUser(request);
    }

    @Test
    void rejectsRolesOutsideDemoAllowlist() {
        KeycloakUserProvisioner provisioner = mock(KeycloakUserProvisioner.class);
        DemoUserService service = new DemoUserService(provisioner);
        DemoUserRequest request = new DemoUserRequest(
                "demo-user",
                "Password123!",
                "auditor",
                "C-1001",
                List.of("A-1001", "A-1002"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.createDemoUser(request));

        assertEquals(400, exception.getStatusCode().value());
        then(provisioner).shouldHaveNoInteractions();
    }
}
