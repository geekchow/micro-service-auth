package com.banking.poc.identitybootstrap.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminProvisionerTest {

    @Test
    void createsMissingUserWithAttributesAndRealmRole() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
                "http://keycloak:8080",
                "banking-poc",
                "master",
                "admin-cli",
                "admin",
                "admin");
        KeycloakAdminProvisioner provisioner = new KeycloakAdminProvisioner(properties, restTemplate, new ObjectMapper());

        server.expect(once(), requestTo("http://keycloak:8080/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=password")))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users?username=alice&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"username\":\"alice\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"email\":\"alice@example.local\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"firstName\":\"alice\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"lastName\":\"Demo\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"demo_managed\":[\"true\"]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"customer_id\":[\"C-1001\"]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"account_ids\":[\"A-1001\"]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"temporary\":false")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "http://keycloak:8080/admin/realms/banking-poc/users/user-123"));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/roles/customer"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "role-1",
                          "name": "customer",
                          "composite": false,
                          "clientRole": false,
                          "containerId": "banking-poc"
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"customer\"")))
                .andRespond(withNoContent());

        provisioner.createDemoUser(new DemoUserRequest(
                "alice",
                "Password123!",
                "customer",
                "C-1001",
                List.of("A-1001")));

        server.verify();
    }

    @Test
    void retriesLookupWhenCreateRacesWithAnotherRequest() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
                "http://keycloak:8080",
                "banking-poc",
                "master",
                "admin-cli",
                "admin",
                "admin");
        KeycloakAdminProvisioner provisioner = new KeycloakAdminProvisioner(properties, restTemplate, new ObjectMapper());

        server.expect(once(), requestTo("http://keycloak:8080/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users?username=alice&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users?username=alice&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "user-123",
                            "username": "alice"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-123",
                          "username": "alice",
                          "attributes": {
                            "demo_managed": ["true"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"demo_managed\":[\"true\"]")))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/roles/customer"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "role-1",
                          "name": "customer",
                          "composite": false,
                          "clientRole": false,
                          "containerId": "banking-poc"
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"customer\"")))
                .andRespond(withNoContent());

        provisioner.createDemoUser(new DemoUserRequest(
                "alice",
                "Password123!",
                "customer",
                "C-1001",
                List.of("A-1001")));

        server.verify();
    }

    @Test
    void updatesExistingUserSoDemoSetupIsIdempotent() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
                "http://keycloak:8080",
                "banking-poc",
                "master",
                "admin-cli",
                "admin",
                "admin");
        KeycloakAdminProvisioner provisioner = new KeycloakAdminProvisioner(properties, restTemplate, new ObjectMapper());

        server.expect(once(), requestTo("http://keycloak:8080/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users?username=ops-admin&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "user-999",
                            "username": "ops-admin"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-999",
                          "username": "ops-admin",
                          "attributes": {
                            "demo_managed": ["true"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-999"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"email\":\"ops-admin@example.local\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"firstName\":\"ops-admin\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"lastName\":\"Demo\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"demo_managed\":[\"true\"]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"customer_id\":[\"C-9999\"]")))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-999/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"value\":\"Password123!\"")))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-999/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/roles/ops-admin"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "role-ops",
                          "name": "ops-admin",
                          "composite": false,
                          "clientRole": false,
                          "containerId": "banking-poc"
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-999/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"ops-admin\"")))
                .andRespond(withNoContent());

        provisioner.createDemoUser(new DemoUserRequest(
                "ops-admin",
                "Password123!",
                "ops-admin",
                "C-9999",
                List.of("A-1001", "A-2001")));

        server.verify();
    }

    @Test
    void refusesToOverwriteExistingUserThatIsNotDemoManaged() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
                "http://keycloak:8080",
                "banking-poc",
                "master",
                "admin-cli",
                "admin",
                "admin");
        KeycloakAdminProvisioner provisioner = new KeycloakAdminProvisioner(properties, restTemplate, new ObjectMapper());

        server.expect(once(), requestTo("http://keycloak:8080/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users?username=alice&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "user-123",
                            "username": "alice"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-123",
                          "username": "alice",
                          "attributes": {
                            "customer_id": ["C-1001"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        org.springframework.web.server.ResponseStatusException exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> provisioner.createDemoUser(new DemoUserRequest(
                        "alice",
                        "Password123!",
                        "customer",
                        "C-1001",
                        List.of("A-1001"))));

        assertEquals(409, exception.getStatusCode().value());
        assertEquals("Existing Keycloak user 'alice' is not managed by the demo bootstrap flow", exception.getReason());
        server.verify();
    }

    @Test
    void replacesExistingRealmRoleWhenReprovisioningWithNarrowerAccess() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
                "http://keycloak:8080",
                "banking-poc",
                "master",
                "admin-cli",
                "admin",
                "admin");
        KeycloakAdminProvisioner provisioner = new KeycloakAdminProvisioner(properties, restTemplate, new ObjectMapper());

        server.expect(once(), requestTo("http://keycloak:8080/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users?username=alice&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "user-123",
                            "username": "alice"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-123",
                          "username": "alice",
                          "attributes": {
                            "demo_managed": ["true"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"demo_managed\":[\"true\"]")))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "role-ops",
                            "name": "ops-admin",
                            "composite": false,
                            "clientRole": false,
                            "containerId": "banking-poc"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"ops-admin\"")))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/roles/customer"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "role-customer",
                          "name": "customer",
                          "composite": false,
                          "clientRole": false,
                          "containerId": "banking-poc"
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"customer\"")))
                .andRespond(withNoContent());

        provisioner.createDemoUser(new DemoUserRequest(
                "alice",
                "Password123!",
                "customer",
                "C-1001",
                List.of("A-1001")));

        server.verify();
    }

    @Test
    void preservesNonDemoRealmRolesWhenReprovisioning() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
                "http://keycloak:8080",
                "banking-poc",
                "master",
                "admin-cli",
                "admin",
                "admin");
        KeycloakAdminProvisioner provisioner = new KeycloakAdminProvisioner(properties, restTemplate, new ObjectMapper());

        server.expect(once(), requestTo("http://keycloak:8080/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users?username=alice&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "user-123",
                            "username": "alice"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-123",
                          "username": "alice",
                          "attributes": {
                            "demo_managed": ["true"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"demo_managed\":[\"true\"]")))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withNoContent());

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "role-auditor",
                            "name": "auditor",
                            "composite": false,
                            "clientRole": false,
                            "containerId": "banking-poc"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/roles/customer"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "role-customer",
                          "name": "customer",
                          "composite": false,
                          "clientRole": false,
                          "containerId": "banking-poc"
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak:8080/admin/realms/banking-poc/users/user-123/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"customer\"")))
                .andRespond(withNoContent());

        provisioner.createDemoUser(new DemoUserRequest(
                "alice",
                "Password123!",
                "customer",
                "C-1001",
                List.of("A-1001")));

        server.verify();
    }
}
