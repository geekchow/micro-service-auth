package com.banking.poc.identitybootstrap.user;

public interface KeycloakUserProvisioner {

    DemoUserCreatedResponse createDemoUser(DemoUserRequest request);
}
