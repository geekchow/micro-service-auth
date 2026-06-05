package com.banking.poc.identitybootstrap.user;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class DemoUserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("customer", "ops-admin");

    private final KeycloakUserProvisioner keycloakUserProvisioner;

    public DemoUserService(KeycloakUserProvisioner keycloakUserProvisioner) {
        this.keycloakUserProvisioner = keycloakUserProvisioner;
    }

    public DemoUserCreatedResponse createDemoUser(DemoUserRequest request) {
        if (!ALLOWED_ROLES.contains(request.role())) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported demo role");
        }

        return keycloakUserProvisioner.createDemoUser(request);
    }
}
