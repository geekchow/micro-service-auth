package com.banking.poc.identitybootstrap.user;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.getField;

class KeycloakAdminConfigurationTest {

    @Test
    void restTemplateUsesConfiguredTimeouts() {
        RestTemplate restTemplate = new KeycloakAdminConfiguration().restTemplate();

        SimpleClientHttpRequestFactory requestFactory =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();

        assertEquals(5000, getField(requestFactory, "connectTimeout"));
        assertEquals(5000, getField(requestFactory, "readTimeout"));
    }
}
