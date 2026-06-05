package com.banking.poc.bankingapi.transaction;

import com.banking.poc.bankingapi.account.AccountAccessGuard;
import com.banking.poc.bankingapi.account.AccountDto;
import com.banking.poc.bankingapi.account.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class TransactionController {

    private final AccountRepository accountRepository;
    private final AccountAccessGuard accountAccessGuard;
    private final TransactionRepository transactionRepository;

    public TransactionController(
            AccountRepository accountRepository,
            AccountAccessGuard accountAccessGuard,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.accountAccessGuard = accountAccessGuard;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionDto> getTransactions(@PathVariable String accountId, @AuthenticationPrincipal Jwt jwt) {
        accountAccessGuard.checkCanAccessAccountId(accountId, jwt);
        AccountDto account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        accountAccessGuard.checkCanAccess(account, jwt);
        return transactionRepository.findByAccountId(accountId);
    }
}
