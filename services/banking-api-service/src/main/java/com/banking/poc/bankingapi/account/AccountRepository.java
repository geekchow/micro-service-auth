package com.banking.poc.bankingapi.account;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class AccountRepository {

    private final Map<String, AccountDto> accounts = Map.of(
            "A-1001", new AccountDto("A-1001", "C-1001", "GBP"),
            "A-2001", new AccountDto("A-2001", "C-2001", "GBP")
    );

    public Optional<AccountDto> findById(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }
}
