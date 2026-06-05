package com.banking.poc.identitybootstrap.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfiguration {

    private static final Duration KEYCLOAK_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(KEYCLOAK_TIMEOUT);
        requestFactory.setReadTimeout(KEYCLOAK_TIMEOUT);
        return new RestTemplate(requestFactory);
    }

    @Bean
    KeycloakUserProvisioner keycloakUserProvisioner(
            KeycloakAdminProperties properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        return new KeycloakAdminProvisioner(properties, restTemplate, objectMapper);
    }
}
