package com.banking.poc.bankingapi.transaction;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Repository
public class TransactionRepository {

    private final Map<String, List<TransactionDto>> transactionsByAccountId = Map.of(
            "A-1001", List.of(
                    new TransactionDto("A-1001", new BigDecimal("-12.35"))
            )
    );

    public List<TransactionDto> findByAccountId(String accountId) {
        return transactionsByAccountId.getOrDefault(accountId, List.of());
    }
}
