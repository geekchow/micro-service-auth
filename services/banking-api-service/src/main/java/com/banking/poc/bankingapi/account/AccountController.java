package com.banking.poc.bankingapi.account;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountAccessGuard accountAccessGuard;

    public AccountController(AccountRepository accountRepository, AccountAccessGuard accountAccessGuard) {
        this.accountRepository = accountRepository;
        this.accountAccessGuard = accountAccessGuard;
    }

    @GetMapping("/{accountId}")
    public AccountDto getAccount(@PathVariable String accountId, @AuthenticationPrincipal Jwt jwt) {
        accountAccessGuard.checkCanAccessAccountId(accountId, jwt);
        AccountDto account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        accountAccessGuard.checkCanAccess(account, jwt);
        return account;
    }
}
