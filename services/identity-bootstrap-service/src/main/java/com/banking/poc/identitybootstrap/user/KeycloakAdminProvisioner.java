package com.banking.poc.identitybootstrap.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.CONFLICT;

public class KeycloakAdminProvisioner implements KeycloakUserProvisioner {

    private static final Set<String> DEMO_MANAGED_ROLES = Set.of("customer", "ops-admin");
    private static final String DEMO_MANAGED_ATTRIBUTE = "demo_managed";
    private static final String DEMO_MANAGED_VALUE = "true";

    private final KeycloakAdminProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KeycloakAdminProvisioner(
            KeycloakAdminProperties properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public DemoUserCreatedResponse createDemoUser(DemoUserRequest request) {
        String adminToken = fetchAdminAccessToken();
        String userId = findUserId(request.username(), adminToken);

        if (userId == null) {
            userId = createUser(request, adminToken);
        } else {
            reconcileExistingUser(userId, request, adminToken);
        }

        syncRealmRole(userId, request.role(), adminToken);
        return new DemoUserCreatedResponse(request.username(), request.role(), "created");
    }

    private String fetchAdminAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.clientId());
        form.add("username", properties.username());
        form.add("password", properties.password());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<String> response = restTemplate.postForEntity(
                realmUri(properties.adminRealm(), "/protocol/openid-connect/token"),
                new HttpEntity<>(form, headers),
                String.class);

        return readJson(response.getBody()).path("access_token").asText(null);
    }

    private String findUserId(String username, String adminToken) {
        URI uri = UriComponentsBuilder
                .fromUriString(adminRealmBaseUri() + "/users")
                .queryParam("username", username)
                .queryParam("exact", true)
                .build(true)
                .toUri();

        ResponseEntity<String> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                authorizedEntity(adminToken),
                String.class);

        JsonNode users = readJson(response.getBody());
        if (!users.isArray() || users.isEmpty()) {
            return null;
        }

        return users.get(0).path("id").asText(null);
    }

    private String createUser(DemoUserRequest request, String adminToken) {
        ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(
                    adminRealmBaseUri() + "/users",
                    HttpMethod.POST,
                    authorizedJsonEntity(userRepresentation(request), adminToken),
                    Void.class);
        } catch (HttpClientErrorException.Conflict exception) {
            String existingUserId = findUserId(request.username(), adminToken);
            if (existingUserId == null) {
                throw exception;
            }

            reconcileExistingUser(existingUserId, request, adminToken);
            return existingUserId;
        }

        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("Keycloak user creation did not return a location header");
        }

        int lastSlash = location.lastIndexOf('/');
        return lastSlash >= 0 ? location.substring(lastSlash + 1) : location;
    }

    private void updateUser(String userId, DemoUserRequest request, String adminToken) {
        restTemplate.exchange(
                adminRealmBaseUri() + "/users/" + userId,
                HttpMethod.PUT,
                authorizedJsonEntity(userProfileUpdate(request), adminToken),
                Void.class);
    }

    private void reconcileExistingUser(String userId, DemoUserRequest request, String adminToken) {
        verifyDemoManagedUser(userId, request.username(), adminToken);
        updateUser(userId, request, adminToken);
        resetPassword(userId, request.password(), adminToken);
    }

    private void verifyDemoManagedUser(String userId, String username, String adminToken) {
        ResponseEntity<String> response = restTemplate.exchange(
                adminRealmBaseUri() + "/users/" + userId,
                HttpMethod.GET,
                authorizedEntity(adminToken),
                String.class);

        JsonNode user = readJson(response.getBody());
        if (!isDemoManaged(user.path("attributes").path(DEMO_MANAGED_ATTRIBUTE))) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Existing Keycloak user '" + username + "' is not managed by the demo bootstrap flow");
        }
    }

    private void resetPassword(String userId, String password, String adminToken) {
        Map<String, Object> passwordPayload = Map.of(
                "type", "password",
                "value", password,
                "temporary", false);

        restTemplate.exchange(
                adminRealmBaseUri() + "/users/" + userId + "/reset-password",
                HttpMethod.PUT,
                authorizedJsonEntity(passwordPayload, adminToken),
                Void.class);
    }

    private void syncRealmRole(String userId, String roleName, String adminToken) {
        List<Map<String, Object>> currentRoles = currentRealmRoles(userId, adminToken);
        List<Map<String, Object>> rolesToRemove = currentRoles.stream()
                .filter(role -> DEMO_MANAGED_ROLES.contains(role.get("name")))
                .filter(role -> !roleName.equals(role.get("name")))
                .toList();
        boolean hasRequestedRole = currentRoles.stream()
                .anyMatch(role -> roleName.equals(role.get("name")));

        if (!rolesToRemove.isEmpty()) {
            restTemplate.exchange(
                    adminRealmBaseUri() + "/users/" + userId + "/role-mappings/realm",
                    HttpMethod.DELETE,
                    authorizedJsonEntity(rolesToRemove, adminToken),
                    Void.class);
        }

        if (!hasRequestedRole) {
            Map<String, Object> requestedRole = realmRole(roleName, adminToken);

            restTemplate.exchange(
                    adminRealmBaseUri() + "/users/" + userId + "/role-mappings/realm",
                    HttpMethod.POST,
                    authorizedJsonEntity(List.of(requestedRole), adminToken),
                    Void.class);
        }
    }

    private List<Map<String, Object>> currentRealmRoles(String userId, String adminToken) {
        ResponseEntity<String> response = restTemplate.exchange(
                adminRealmBaseUri() + "/users/" + userId + "/role-mappings/realm",
                HttpMethod.GET,
                authorizedEntity(adminToken),
                String.class);

        JsonNode roles = readJson(response.getBody());
        List<Map<String, Object>> roleMappings = new ArrayList<>();
        if (!roles.isArray()) {
            return roleMappings;
        }

        for (JsonNode role : roles) {
            roleMappings.add(objectMapper.convertValue(role, Map.class));
        }

        return roleMappings;
    }

    private Map<String, Object> realmRole(String roleName, String adminToken) {
        ResponseEntity<String> response = restTemplate.exchange(
                adminRealmBaseUri() + "/roles/" + roleName,
                HttpMethod.GET,
                authorizedEntity(adminToken),
                String.class);

        JsonNode role = readJson(response.getBody());
        return objectMapper.convertValue(role, Map.class);
    }

    private Map<String, Object> userRepresentation(DemoUserRequest request) {
        Map<String, Object> representation = new LinkedHashMap<>();
        representation.put("username", request.username());
        representation.put("enabled", true);
        representation.put("email", emailFor(request));
        representation.put("firstName", request.username());
        representation.put("lastName", "Demo");
        representation.put("attributes", attributes(request));
        representation.put("credentials", List.of(Map.of(
                "type", "password",
                "value", request.password(),
                "temporary", false)));
        return representation;
    }

    private Map<String, Object> userProfileUpdate(DemoUserRequest request) {
        Map<String, Object> representation = new LinkedHashMap<>();
        representation.put("username", request.username());
        representation.put("enabled", true);
        representation.put("email", emailFor(request));
        representation.put("firstName", request.username());
        representation.put("lastName", "Demo");
        representation.put("attributes", attributes(request));
        return representation;
    }

    private String emailFor(DemoUserRequest request) {
        return request.username() + "@example.local";
    }

    private Map<String, List<String>> attributes(DemoUserRequest request) {
        return Map.of(
                DEMO_MANAGED_ATTRIBUTE, List.of(DEMO_MANAGED_VALUE),
                "customer_id", List.of(request.customerId()),
                "account_ids", request.accountIds());
    }

    private boolean isDemoManaged(JsonNode markerNode) {
        if (markerNode.isArray()) {
            for (JsonNode value : markerNode) {
                if (DEMO_MANAGED_VALUE.equalsIgnoreCase(value.asText())) {
                    return true;
                }
            }
            return false;
        }

        return DEMO_MANAGED_VALUE.equalsIgnoreCase(markerNode.asText());
    }

    private HttpEntity<Void> authorizedEntity(String adminToken) {
        return new HttpEntity<>(authorizationHeaders(adminToken));
    }

    private HttpEntity<Object> authorizedJsonEntity(Object body, String adminToken) {
        HttpHeaders headers = authorizationHeaders(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders authorizationHeaders(String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return headers;
    }

    private String realmUri(String realm, String suffix) {
        return properties.serverUrl() + "/realms/" + realm + suffix;
    }

    private String adminRealmBaseUri() {
        return properties.serverUrl() + "/admin/realms/" + properties.realm();
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body == null ? "null" : body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse Keycloak response", exception);
        }
    }
}
