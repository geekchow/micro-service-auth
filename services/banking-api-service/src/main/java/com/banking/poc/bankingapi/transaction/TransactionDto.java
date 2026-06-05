package com.banking.poc.bankingapi.transaction;

import java.math.BigDecimal;

public record TransactionDto(String accountId, BigDecimal amount) {
}
