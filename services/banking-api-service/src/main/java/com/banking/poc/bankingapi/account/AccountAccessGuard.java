package com.banking.poc.bankingapi.account;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Component
public class AccountAccessGuard {

    public void checkCanAccessAccountId(String accountId, Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = realmRoles(jwt);
        if (roles.contains("ops-admin")) {
            return;
        }

        List<String> accountIds = accountIds(jwt);
        String customerId = jwt.getClaimAsString("customer_id");
        if (roles.contains("customer")
                && customerId != null
                && !customerId.isBlank()
                && accountIds.contains(accountId)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public void checkCanAccess(AccountDto account, Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = realmRoles(jwt);
        List<String> accountIds = accountIds(jwt);
        String customerId = jwt.getClaimAsString("customer_id");

        if (roles.contains("ops-admin")) {
            return;
        }

        if (roles.contains("customer")
                && customerId != null
                && !customerId.isBlank()
                && account.customerId().equals(customerId)
                && accountIds.contains(account.accountId())) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private List<String> realmRoles(Jwt jwt) {
        Object realmAccessClaim = jwt.getClaim("realm_access");
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return List.of();
        }

        Object rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof List<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private List<String> accountIds(Jwt jwt) {
        List<String> accountIds = jwt.getClaimAsStringList("account_ids");
        return accountIds == null ? List.of() : accountIds;
    }
}
