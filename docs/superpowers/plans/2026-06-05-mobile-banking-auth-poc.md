# Mobile Banking Auth PoC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local, dockerized proof of concept where Keycloak issues JWTs, Kong enforces API access as the edge PEP, OPA makes account-access decisions as the PDP, and Spring Boot microservices handle user setup and retail banking APIs.

**Architecture:** Use two Spring Boot services in a multi-module Maven repo. `banking-api-service` is a Spring Security resource server that validates Keycloak JWTs and serves in-memory account data. `identity-bootstrap-service` creates demo users in Keycloak. Kong proxies protected API routes and calls OPA through a lightweight custom Lua plugin that extracts bearer-token claims for policy evaluation, while Spring Boot provides the cryptographic JWT verification required by the PoC.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Security OAuth2 Resource Server, Maven multi-module build, Keycloak 26, Kong 3.x DB-less mode, OPA/Rego, Docker Compose, JUnit 5, MockMvc

---

## File Structure

- Create: `pom.xml`
  - Maven parent POM for the multi-module build
- Create: `docker-compose.yml`
  - Local runtime topology for Keycloak, Kong, OPA, and the two Spring Boot services
- Create: `services/banking-api-service/pom.xml`
  - Banking service dependencies and build config
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/BankingApiServiceApplication.java`
  - Banking service entrypoint
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountDto.java`
  - Account response payload
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountRepository.java`
  - In-memory account store
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountController.java`
  - Account detail endpoint
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionDto.java`
  - Transaction response payload
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionRepository.java`
  - In-memory transaction store
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionController.java`
  - Account transactions endpoint
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/security/SecurityConfig.java`
  - Spring Security resource-server config
- Create: `services/banking-api-service/src/main/resources/application.yml`
  - JWT issuer and service settings
- Create: `services/banking-api-service/src/test/java/com/banking/poc/bankingapi/BankingApiServiceApplicationTests.java`
  - Context smoke test
- Create: `services/banking-api-service/src/test/java/com/banking/poc/bankingapi/account/AccountControllerIntegrationTest.java`
  - Account endpoint tests
- Create: `services/banking-api-service/src/test/java/com/banking/poc/bankingapi/transaction/TransactionControllerIntegrationTest.java`
  - Transactions endpoint tests
- Create: `services/banking-api-service/src/test/java/com/banking/poc/bankingapi/security/BankingSecurityIntegrationTest.java`
  - Auth-required endpoint tests
- Create: `services/banking-api-service/Dockerfile`
  - Container image for the banking API
- Create: `services/identity-bootstrap-service/pom.xml`
  - Identity bootstrap service dependencies and build config
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/IdentityBootstrapServiceApplication.java`
  - Identity service entrypoint
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserRequest.java`
  - Demo user setup request payload
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserCreatedResponse.java`
  - Demo user setup response payload
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakUserProvisioner.java`
  - Provisioning abstraction for Keycloak operations
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminProperties.java`
  - Config properties for Keycloak admin access
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminConfiguration.java`
  - Keycloak admin client bean
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminProvisioner.java`
  - Keycloak-backed user provisioning implementation
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserService.java`
  - Orchestrates user creation
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserController.java`
  - `POST /demo/users`
- Create: `services/identity-bootstrap-service/src/main/resources/application.yml`
  - Keycloak admin connection settings
- Create: `services/identity-bootstrap-service/src/test/java/com/banking/poc/identitybootstrap/user/DemoUserControllerTest.java`
  - Controller behavior tests
- Create: `services/identity-bootstrap-service/src/test/java/com/banking/poc/identitybootstrap/user/DemoUserServiceTest.java`
  - Service orchestration tests
- Create: `services/identity-bootstrap-service/Dockerfile`
  - Container image for the bootstrap service
- Create: `infra/opa/policies/banking_authz.rego`
  - Retail account authorization policy
- Create: `infra/opa/policies/banking_authz_test.rego`
  - Rego tests for allow and deny cases
- Create: `infra/kong/kong.yml`
  - Kong DB-less routes, services, and plugin config
- Create: `infra/kong/plugins/opa-authz/schema.lua`
  - Kong custom plugin schema
- Create: `infra/kong/plugins/opa-authz/handler.lua`
  - Kong custom plugin that calls OPA
- Create: `infra/keycloak/realm-export.json`
  - Imported realm, roles, and public client setup
- Create: `scripts/demo.sh`
  - End-to-end PoC proof script

### Task 1: Banking Service Skeleton

**Files:**
- Create: `pom.xml`
- Create: `services/banking-api-service/pom.xml`
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/BankingApiServiceApplication.java`
- Create: `services/banking-api-service/src/test/java/com/banking/poc/bankingapi/BankingApiServiceApplicationTests.java`

- [ ] **Step 1: Write the failing test**

```java
package com.banking.poc.bankingapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BankingApiServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl services/banking-api-service test`
Expected: FAIL with a Maven error such as `Could not find the selected project in the reactor` because the parent POM and module do not exist yet.

- [ ] **Step 3: Write minimal implementation**

`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.banking.poc</groupId>
    <artifactId>mobile-banking-auth-poc</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>services/banking-api-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.3.1</spring-boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

`services/banking-api-service/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.banking.poc</groupId>
        <artifactId>mobile-banking-auth-poc</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>banking-api-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

`services/banking-api-service/src/main/java/com/banking/poc/bankingapi/BankingApiServiceApplication.java`

```java
package com.banking.poc.bankingapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankingApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingApiServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl services/banking-api-service test`
Expected: PASS with `BUILD SUCCESS` and `BankingApiServiceApplicationTests` green.

- [ ] **Step 5: Commit**

```bash
git init
git add pom.xml services/banking-api-service/pom.xml services/banking-api-service/src/main/java/com/banking/poc/bankingapi/BankingApiServiceApplication.java services/banking-api-service/src/test/java/com/banking/poc/bankingapi/BankingApiServiceApplicationTests.java
git commit -m "chore: scaffold banking api service"
```

### Task 2: Account Details Endpoint

**Files:**
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountDto.java`
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountRepository.java`
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountController.java`
- Create: `services/banking-api-service/src/test/java/com/banking/poc/bankingapi/account/AccountControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.banking.poc.bankingapi.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsAccountDetailsForKnownAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/A-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("A-1001"))
                .andExpect(jsonPath("$.customerId").value("C-1001"))
                .andExpect(jsonPath("$.currency").value("GBP"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl services/banking-api-service -Dtest=AccountControllerIntegrationTest test`
Expected: FAIL with `404` because `/api/accounts/A-1001` is not implemented yet.

- [ ] **Step 3: Write minimal implementation**

`services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountDto.java`

```java
package com.banking.poc.bankingapi.account;

import java.math.BigDecimal;

public record AccountDto(
        String accountId,
        String customerId,
        String ownerName,
        BigDecimal availableBalance,
        String currency
) {
}
```

`services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountRepository.java`

```java
package com.banking.poc.bankingapi.account;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Repository
public class AccountRepository {

    private final Map<String, AccountDto> accounts = Map.of(
            "A-1001", new AccountDto("A-1001", "C-1001", "Alice", new BigDecimal("1520.45"), "GBP"),
            "A-2001", new AccountDto("A-2001", "C-2001", "Bob", new BigDecimal("980.11"), "GBP")
    );

    public Optional<AccountDto> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }
}
```

`services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountController.java`

```java
package com.banking.poc.bankingapi.account;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String accountId) {
        return accountRepository.findById(accountId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl services/banking-api-service -Dtest=AccountControllerIntegrationTest test`
Expected: PASS with one green test.

- [ ] **Step 5: Commit**

```bash
git add services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountDto.java services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountRepository.java services/banking-api-service/src/main/java/com/banking/poc/bankingapi/account/AccountController.java services/banking-api-service/src/test/java/com/banking/poc/bankingapi/account/AccountControllerIntegrationTest.java
git commit -m "feat: add account details endpoint"
```

### Task 3: Transactions Endpoint

**Files:**
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionDto.java`
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionRepository.java`
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionController.java`
- Create: `services/banking-api-service/src/test/java/com/banking/poc/bankingapi/transaction/TransactionControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.banking.poc.bankingapi.transaction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTransactionsForKnownAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/A-1001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value("A-1001"))
                .andExpect(jsonPath("$[0].amount").value("-12.35"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl services/banking-api-service -Dtest=TransactionControllerIntegrationTest test`
Expected: FAIL with `404` because the transactions endpoint does not exist yet.

- [ ] **Step 3: Write minimal implementation**

`services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionDto.java`

```java
package com.banking.poc.bankingapi.transaction;

import java.math.BigDecimal;

public record TransactionDto(
        String transactionId,
        String accountId,
        String bookingDate,
        BigDecimal amount,
        String description
) {
}
```

`services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionRepository.java`

```java
package com.banking.poc.bankingapi.transaction;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class TransactionRepository {

    private final Map<String, List<TransactionDto>> transactions = Map.of(
            "A-1001", List.of(
                    new TransactionDto("T-10001", "A-1001", "2026-06-01", new BigDecimal("-12.35"), "Coffee Shop"),
                    new TransactionDto("T-10002", "A-1001", "2026-06-02", new BigDecimal("1200.00"), "Salary")
            ),
            "A-2001", List.of(
                    new TransactionDto("T-20001", "A-2001", "2026-06-02", new BigDecimal("-55.10"), "Groceries")
            )
    );

    public List<TransactionDto> findByAccountId(String accountId) {
        return transactions.getOrDefault(accountId, List.of());
    }
}
```

`services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionController.java`

```java
package com.banking.poc.bankingapi.transaction;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionDto> getTransactions(@PathVariable String accountId) {
        return transactionRepository.findByAccountId(accountId);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl services/banking-api-service -Dtest=TransactionControllerIntegrationTest test`
Expected: PASS with one green test.

- [ ] **Step 5: Commit**

```bash
git add services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionDto.java services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionRepository.java services/banking-api-service/src/main/java/com/banking/poc/bankingapi/transaction/TransactionController.java services/banking-api-service/src/test/java/com/banking/poc/bankingapi/transaction/TransactionControllerIntegrationTest.java
git commit -m "feat: add transactions endpoint"
```

### Task 4: Secure Banking APIs With JWT Authentication

**Files:**
- Modify: `services/banking-api-service/pom.xml`
- Create: `services/banking-api-service/src/main/java/com/banking/poc/bankingapi/security/SecurityConfig.java`
- Create: `services/banking-api-service/src/main/resources/application.yml`
- Create: `services/banking-api-service/src/test/java/com/banking/poc/bankingapi/security/BankingSecurityIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.banking.poc.bankingapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/mock/jwks")
@AutoConfigureMockMvc
class BankingSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/accounts/A-1001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsRequestsWithJwtAuthentication() throws Exception {
        mockMvc.perform(get("/api/accounts/A-1001")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl services/banking-api-service -Dtest=BankingSecurityIntegrationTest test`
Expected: FAIL because the endpoint still returns `200` without authentication.

- [ ] **Step 3: Write minimal implementation**

`services/banking-api-service/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.banking.poc</groupId>
        <artifactId>mobile-banking-auth-poc</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>banking-api-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

`services/banking-api-service/src/main/java/com/banking/poc/bankingapi/security/SecurityConfig.java`

```java
package com.banking.poc.bankingapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
```

`services/banking-api-service/src/main/resources/application.yml`

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8081/realms/banking-poc}
management:
  endpoints:
    web:
      exposure:
        include: health
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl services/banking-api-service -Dtest=BankingSecurityIntegrationTest test`
Expected: PASS with both tests green.

- [ ] **Step 5: Commit**

```bash
git add services/banking-api-service/pom.xml services/banking-api-service/src/main/java/com/banking/poc/bankingapi/security/SecurityConfig.java services/banking-api-service/src/main/resources/application.yml services/banking-api-service/src/test/java/com/banking/poc/bankingapi/security/BankingSecurityIntegrationTest.java
git commit -m "feat: secure banking api with jwt auth"
```

### Task 5: Identity Bootstrap Service For Demo User Setup

**Files:**
- Modify: `pom.xml`
- Create: `services/identity-bootstrap-service/pom.xml`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/IdentityBootstrapServiceApplication.java`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserRequest.java`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserCreatedResponse.java`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakUserProvisioner.java`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminProperties.java`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminConfiguration.java`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminProvisioner.java`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserService.java`
- Create: `services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserController.java`
- Create: `services/identity-bootstrap-service/src/main/resources/application.yml`
- Create: `services/identity-bootstrap-service/src/test/java/com/banking/poc/identitybootstrap/user/DemoUserControllerTest.java`
- Create: `services/identity-bootstrap-service/src/test/java/com/banking/poc/identitybootstrap/user/DemoUserServiceTest.java`

- [ ] **Step 1: Write the failing test**

`services/identity-bootstrap-service/src/test/java/com/banking/poc/identitybootstrap/user/DemoUserControllerTest.java`

```java
package com.banking.poc.identitybootstrap.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoUserController.class)
class DemoUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoUserService demoUserService;

    @Test
    void createsDemoUser() throws Exception {
        when(demoUserService.createUser(any())).thenReturn(new DemoUserCreatedResponse("alice", "customer", "created"));

        mockMvc.perform(post("/demo/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"username\": \"alice\",
                                  \"password\": \"Password123!\",
                                  \"role\": \"customer\",
                                  \"customerId\": \"C-1001\",
                                  \"accountIds\": [\"A-1001\"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.status").value("created"));
    }
}
```

`services/identity-bootstrap-service/src/test/java/com/banking/poc/identitybootstrap/user/DemoUserServiceTest.java`

```java
package com.banking.poc.identitybootstrap.user;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DemoUserServiceTest {

    @Test
    void delegatesProvisioningToKeycloakProvisioner() {
        KeycloakUserProvisioner provisioner = mock(KeycloakUserProvisioner.class);
        DemoUserService service = new DemoUserService(provisioner);
        DemoUserRequest request = new DemoUserRequest("alice", "Password123!", "customer", "C-1001", List.of("A-1001"));

        DemoUserCreatedResponse response = service.createUser(request);

        verify(provisioner).createUser(request);
        assertThat(response.status()).isEqualTo("created");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl services/identity-bootstrap-service test`
Expected: FAIL because the new module and controller classes do not exist yet.

- [ ] **Step 3: Write minimal implementation**

`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.banking.poc</groupId>
    <artifactId>mobile-banking-auth-poc</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>services/banking-api-service</module>
        <module>services/identity-bootstrap-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.3.1</spring-boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

`services/identity-bootstrap-service/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.banking.poc</groupId>
        <artifactId>mobile-banking-auth-poc</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>identity-bootstrap-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-admin-client</artifactId>
            <version>25.0.1</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/IdentityBootstrapServiceApplication.java`

```java
package com.banking.poc.identitybootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IdentityBootstrapServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityBootstrapServiceApplication.class, args);
    }
}
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserRequest.java`

```java
package com.banking.poc.identitybootstrap.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DemoUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String role,
        @NotBlank String customerId,
        @NotEmpty List<String> accountIds
) {
}
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserCreatedResponse.java`

```java
package com.banking.poc.identitybootstrap.user;

public record DemoUserCreatedResponse(String username, String role, String status) {
}
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakUserProvisioner.java`

```java
package com.banking.poc.identitybootstrap.user;

public interface KeycloakUserProvisioner {
    void createUser(DemoUserRequest request);
}
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminProperties.java`

```java
package com.banking.poc.identitybootstrap.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
        String serverUrl,
        String realm,
        String username,
        String password
) {
}
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminConfiguration.java`

```java
package com.banking.poc.identitybootstrap.user;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfiguration {

    @Bean
    Keycloak keycloakAdminClient(KeycloakAdminProperties properties) {
        return KeycloakBuilder.builder()
                .serverUrl(properties.serverUrl())
                .realm("master")
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli")
                .username(properties.username())
                .password(properties.password())
                .build();
    }
}
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/KeycloakAdminProvisioner.java`

```java
package com.banking.poc.identitybootstrap.user;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KeycloakAdminProvisioner implements KeycloakUserProvisioner {

    private final Keycloak keycloak;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminProvisioner(Keycloak keycloak, KeycloakAdminProperties properties) {
        this.keycloak = keycloak;
        this.properties = properties;
    }

    @Override
    public void createUser(DemoUserRequest request) {
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(request.password());
        password.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username());
        user.setEnabled(true);
        user.setCredentials(List.of(password));
        user.setRealmRoles(List.of(request.role()));
        user.setAttributes(Map.of(
                "customer_id", List.of(request.customerId()),
                "account_ids", request.accountIds()
        ));

        keycloak.realm(properties.realm()).users().create(user);
    }
}
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserService.java`

```java
package com.banking.poc.identitybootstrap.user;

import org.springframework.stereotype.Service;

@Service
public class DemoUserService {

    private final KeycloakUserProvisioner provisioner;

    public DemoUserService(KeycloakUserProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    public DemoUserCreatedResponse createUser(DemoUserRequest request) {
        provisioner.createUser(request);
        return new DemoUserCreatedResponse(request.username(), request.role(), "created");
    }
}
```

`services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/DemoUserController.java`

```java
package com.banking.poc.identitybootstrap.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo/users")
public class DemoUserController {

    private final DemoUserService demoUserService;

    public DemoUserController(DemoUserService demoUserService) {
        this.demoUserService = demoUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemoUserCreatedResponse createUser(@Valid @RequestBody DemoUserRequest request) {
        return demoUserService.createUser(request);
    }
}
```

`services/identity-bootstrap-service/src/main/resources/application.yml`

```yaml
keycloak:
  admin:
    server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8081}
    realm: ${KEYCLOAK_REALM:banking-poc}
    username: ${KEYCLOAK_ADMIN_USERNAME:admin}
    password: ${KEYCLOAK_ADMIN_PASSWORD:admin}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl services/identity-bootstrap-service test`
Expected: PASS with both `DemoUserControllerTest` and `DemoUserServiceTest` green.

- [ ] **Step 5: Commit**

```bash
git add pom.xml services/identity-bootstrap-service
git commit -m "feat: add identity bootstrap service"
```

### Task 6: OPA Account Authorization Policy

**Files:**
- Create: `infra/opa/policies/banking_authz.rego`
- Create: `infra/opa/policies/banking_authz_test.rego`

- [ ] **Step 1: Write the failing test**

`infra/opa/policies/banking_authz_test.rego`

```rego
package banking.authz

test_customer_can_access_own_account if {
    allow with input as {
        "roles": ["customer"],
        "customer_id": "C-1001",
        "account_id": "A-1001"
    }
}

test_customer_cannot_access_other_account if {
    not allow with input as {
        "roles": ["customer"],
        "customer_id": "C-1001",
        "account_id": "A-2001"
    }
}

test_ops_admin_can_access_any_account if {
    allow with input as {
        "roles": ["ops-admin"],
        "customer_id": "C-1001",
        "account_id": "A-2001"
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `opa test infra/opa/policies -v`
Expected: FAIL because `allow` is not defined yet.

- [ ] **Step 3: Write minimal implementation**

`infra/opa/policies/banking_authz.rego`

```rego
package banking.authz

default allow := false

account_owners := {
    "A-1001": "C-1001",
    "A-2001": "C-2001",
}

allow if "ops-admin" in input.roles

allow if {
    "customer" in input.roles
    account_owners[input.account_id] == input.customer_id
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `opa test infra/opa/policies -v`
Expected: PASS with all three Rego tests green.

- [ ] **Step 5: Commit**

```bash
git add infra/opa/policies/banking_authz.rego infra/opa/policies/banking_authz_test.rego
git commit -m "feat: add opa banking authorization policy"
```

### Task 7: Kong PEP, Keycloak Realm, Containers, And End-to-End Demo

**Files:**
- Create: `docker-compose.yml`
- Create: `infra/kong/kong.yml`
- Create: `infra/kong/plugins/opa-authz/schema.lua`
- Create: `infra/kong/plugins/opa-authz/handler.lua`
- Create: `infra/keycloak/realm-export.json`
- Create: `services/banking-api-service/Dockerfile`
- Create: `services/identity-bootstrap-service/Dockerfile`
- Create: `scripts/demo.sh`

- [ ] **Step 1: Write the failing end-to-end proof script**

```bash
#!/usr/bin/env bash
set -euo pipefail

get_token() {
  local username="$1"
  local password="$2"

  curl -sS -X POST "http://localhost:8081/realms/banking-poc/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=password" \
    --data-urlencode "client_id=mobile-banking-app" \
    --data-urlencode "username=${username}" \
    --data-urlencode "password=${password}"
}

echo "Creating users"
curl -sS -X POST "http://localhost:8082/demo/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Password123!","role":"customer","customerId":"C-1001","accountIds":["A-1001"]}'

curl -sS -X POST "http://localhost:8082/demo/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"ops-admin","password":"Password123!","role":"ops-admin","customerId":"C-9999","accountIds":["A-1001","A-2001"]}'

ALICE_TOKEN_JSON=$(get_token "alice" "Password123!")
OPS_TOKEN_JSON=$(get_token "ops-admin" "Password123!")

ALICE_TOKEN=$(printf '%s' "$ALICE_TOKEN_JSON" | jq -r '.access_token')
OPS_TOKEN=$(printf '%s' "$OPS_TOKEN_JSON" | jq -r '.access_token')

echo "Alice own account should be 200"
curl -i -sS "http://localhost:8000/api/accounts/A-1001" -H "Authorization: Bearer ${ALICE_TOKEN}" | grep "200"

echo "Alice other account should be 403"
curl -i -sS "http://localhost:8000/api/accounts/A-2001" -H "Authorization: Bearer ${ALICE_TOKEN}" | grep "403"

echo "Ops admin any account should be 200"
curl -i -sS "http://localhost:8000/api/accounts/A-2001" -H "Authorization: Bearer ${OPS_TOKEN}" | grep "200"
```

- [ ] **Step 2: Run the proof script to verify it fails**

Run: `bash scripts/demo.sh`
Expected: FAIL immediately with `No such file or directory` or connection-refused errors because the infrastructure is not wired yet.

- [ ] **Step 3: Write minimal infrastructure implementation**

`infra/kong/plugins/opa-authz/schema.lua`

```lua
return {
  name = "opa-authz",
  fields = {
    { config = {
        type = "record",
        fields = {
          { opa_url = { type = "string", required = true } },
          { timeout_ms = { type = "number", default = 2000 } },
        },
      },
    },
  },
}
```

`infra/kong/plugins/opa-authz/handler.lua`

```lua
local cjson = require "cjson.safe"
local http = require "resty.http"

local OpaAuthz = {
  PRIORITY = 900,
  VERSION = "0.1.0",
}

local function decode_segment(segment)
  local rem = #segment % 4
  if rem > 0 then
    segment = segment .. string.rep("=", 4 - rem)
  end
  segment = segment:gsub("-", "+"):gsub("_", "/")
  return ngx.decode_base64(segment)
end

local function decode_payload(token)
  local payload_segment = token:match("^[^.]+%.([^.]+)%.?.*$")
  if not payload_segment then
    return nil
  end
  local decoded = decode_segment(payload_segment)
  return decoded and cjson.decode(decoded) or nil
end

function OpaAuthz:access(conf)
  local auth = kong.request.get_header("authorization")
  if not auth then
    return kong.response.exit(401, { message = "missing bearer token" })
  end

  local token = auth:match("[Bb]earer%s+(.+)")
  if not token then
    return kong.response.exit(401, { message = "invalid bearer token" })
  end

  local claims = decode_payload(token)
  if not claims then
    return kong.response.exit(401, { message = "unreadable jwt payload" })
  end

  local account_id = kong.request.get_path():match("/api/accounts/([^/]+)")
  local body = cjson.encode({
    input = {
      method = kong.request.get_method(),
      path = kong.request.get_path(),
      account_id = account_id,
      customer_id = claims.customer_id,
      roles = claims.realm_access and claims.realm_access.roles or {},
      username = claims.preferred_username,
    }
  })

  local httpc = http.new()
  httpc:set_timeout(conf.timeout_ms)
  local response, err = httpc:request_uri(conf.opa_url, {
    method = "POST",
    body = body,
    headers = { ["Content-Type"] = "application/json" },
  })

  if not response then
    return kong.response.exit(503, { message = "opa unavailable", detail = err })
  end

  local decision = cjson.decode(response.body)
  if response.status ~= 200 or not decision or not decision.result then
    return kong.response.exit(503, { message = "opa error" })
  end

  if decision.result ~= true then
    return kong.response.exit(403, { message = "forbidden" })
  end
end

return OpaAuthz
```

`infra/kong/kong.yml`

```yaml
_format_version: "3.0"
services:
  - name: banking-api
    url: http://banking-api-service:8080
    routes:
      - name: banking-api-route
        paths:
          - /api/accounts
    plugins:
      - name: opa-authz
        config:
          opa_url: http://opa:8181/v1/data/banking/authz/allow
          timeout_ms: 2000
```

`infra/keycloak/realm-export.json`

```json
{
  "realm": "banking-poc",
  "enabled": true,
  "roles": {
    "realm": [
      { "name": "customer" },
      { "name": "ops-admin" }
    ]
  },
  "clients": [
    {
      "clientId": "mobile-banking-app",
      "enabled": true,
      "publicClient": true,
      "directAccessGrantsEnabled": true,
      "protocolMappers": [
        {
          "name": "customer_id",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-attribute-mapper",
          "consentRequired": false,
          "config": {
            "userinfo.token.claim": "true",
            "user.attribute": "customer_id",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "customer_id",
            "jsonType.label": "String"
          }
        }
      ]
    }
  ]
}
```

`services/banking-api-service/Dockerfile`

```dockerfile
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services/banking-api-service/pom.xml services/banking-api-service/pom.xml
COPY services/banking-api-service/src services/banking-api-service/src
RUN mvn -q -pl services/banking-api-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/banking-api-service/target/banking-api-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

`services/identity-bootstrap-service/Dockerfile`

```dockerfile
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services/identity-bootstrap-service/pom.xml services/identity-bootstrap-service/pom.xml
COPY services/identity-bootstrap-service/src services/identity-bootstrap-service/src
RUN mvn -q -pl services/identity-bootstrap-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/identity-bootstrap-service/target/identity-bootstrap-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

`docker-compose.yml`

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    command: ["start-dev", "--import-realm"]
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8081:8080"
    volumes:
      - ./infra/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json:ro

  opa:
    image: openpolicyagent/opa:0.68.0
    command: ["run", "--server", "--addr=0.0.0.0:8181", "/policies"]
    ports:
      - "8181:8181"
    volumes:
      - ./infra/opa/policies:/policies:ro

  banking-api-service:
    build:
      context: .
      dockerfile: services/banking-api-service/Dockerfile
    environment:
      JWT_ISSUER_URI: http://keycloak:8080/realms/banking-poc
    depends_on:
      - keycloak

  identity-bootstrap-service:
    build:
      context: .
      dockerfile: services/identity-bootstrap-service/Dockerfile
    environment:
      KEYCLOAK_SERVER_URL: http://keycloak:8080
      KEYCLOAK_REALM: banking-poc
      KEYCLOAK_ADMIN_USERNAME: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8082:8080"
    depends_on:
      - keycloak

  kong:
    image: kong:3.7
    environment:
      KONG_DATABASE: off
      KONG_DECLARATIVE_CONFIG: /opt/kong/kong.yml
      KONG_PROXY_ACCESS_LOG: /dev/stdout
      KONG_ADMIN_ACCESS_LOG: /dev/stdout
      KONG_PROXY_ERROR_LOG: /dev/stderr
      KONG_ADMIN_ERROR_LOG: /dev/stderr
      KONG_ADMIN_LISTEN: 0.0.0.0:8001
      KONG_PLUGINS: bundled,opa-authz
      KONG_LUA_PACKAGE_PATH: /opt/kong/plugins/?.lua;/opt/kong/plugins/?/init.lua;;
    ports:
      - "8000:8000"
      - "8001:8001"
    depends_on:
      - banking-api-service
      - opa
    volumes:
      - ./infra/kong/kong.yml:/opt/kong/kong.yml:ro
      - ./infra/kong/plugins:/opt/kong/plugins:ro
```

`scripts/demo.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

get_token() {
  local username="$1"
  local password="$2"

  curl -sS -X POST "http://localhost:8081/realms/banking-poc/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=password" \
    --data-urlencode "client_id=mobile-banking-app" \
    --data-urlencode "username=${username}" \
    --data-urlencode "password=${password}"
}

echo "Creating alice"
curl -sS -X POST "http://localhost:8082/demo/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Password123!","role":"customer","customerId":"C-1001","accountIds":["A-1001"]}' >/dev/null

echo "Creating ops-admin"
curl -sS -X POST "http://localhost:8082/demo/users" \
  -H "Content-Type: application/json" \
  -d '{"username":"ops-admin","password":"Password123!","role":"ops-admin","customerId":"C-9999","accountIds":["A-1001","A-2001"]}' >/dev/null

ALICE_TOKEN=$(get_token "alice" "Password123!" | jq -r '.access_token')
OPS_TOKEN=$(get_token "ops-admin" "Password123!" | jq -r '.access_token')

echo "Alice own account should return 200"
curl -i -sS "http://localhost:8000/api/accounts/A-1001" -H "Authorization: Bearer ${ALICE_TOKEN}" | grep "200 OK"

echo "Alice foreign account should return 403"
curl -i -sS "http://localhost:8000/api/accounts/A-2001" -H "Authorization: Bearer ${ALICE_TOKEN}" | grep "403 Forbidden"

echo "Ops admin should return 200"
curl -i -sS "http://localhost:8000/api/accounts/A-2001" -H "Authorization: Bearer ${OPS_TOKEN}" | grep "200 OK"

echo "Missing token should return 401"
curl -i -sS "http://localhost:8000/api/accounts/A-1001" | grep "401 Unauthorized"

TAMPERED_TOKEN="${ALICE_TOKEN}x"
echo "Tampered token should return 401"
curl -i -sS "http://localhost:8000/api/accounts/A-1001" -H "Authorization: Bearer ${TAMPERED_TOKEN}" | grep "401 Unauthorized"
```

- [ ] **Step 4: Run validation and demo to verify it passes**

Run: `docker compose config`
Expected: PASS with the fully rendered stack configuration.

Run: `chmod +x scripts/demo.sh && docker compose up -d --build`
Expected: PASS with all five containers starting.

Run: `bash scripts/demo.sh`
Expected: PASS with the script printing five successful checks: `200 OK`, `403 Forbidden`, `200 OK`, `401 Unauthorized`, `401 Unauthorized`.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml infra/kong infra/keycloak infra/opa services/banking-api-service/Dockerfile services/identity-bootstrap-service/Dockerfile scripts/demo.sh
git commit -m "feat: wire kong opa keycloak poc stack"
```

## Self-Review Notes

- Spec coverage:
  - Spring Boot microservices: covered by Tasks 1 through 5.
  - User setup and login: covered by Task 5 and Task 7.
  - Kong as PEP: covered by Task 7.
  - OPA as separated PDP: covered by Task 6 and Task 7.
  - Keycloak as IdP: covered by Task 5 and Task 7.
  - JWT signature verification: covered by Task 4 runtime configuration and Task 7 end-to-end checks with both Keycloak-issued and tampered tokens.
  - Missing-token and deny-path proofs: covered explicitly by Task 7 end-to-end checks.
- Placeholder scan:
  - No `TBD`, `TODO`, or deferred implementation markers remain.
- Type consistency:
  - Account IDs and customer IDs use the same string model throughout the plan.
  - Keycloak user creation payload and OPA policy input use matching field names: `customerId` at the REST layer and `customer_id` once mapped into token claims and OPA input.
