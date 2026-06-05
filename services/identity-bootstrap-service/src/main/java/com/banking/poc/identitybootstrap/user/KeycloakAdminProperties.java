package com.banking.poc.identitybootstrap.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
        String serverUrl,
        String realm,
        String adminRealm,
        String clientId,
        String username,
        String password) {
}
