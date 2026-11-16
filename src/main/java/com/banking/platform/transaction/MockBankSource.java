package com.banking.platform.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MockBankSource {
    public record ExternalTransaction(String source, String externalId, Instant at, BigDecimal amount, String currency,
                                      String description, String merchant) {
    }

    public List<ExternalTransaction> fetch(String customerId) {
        // Fixed timestamps make the challenge deterministic and reproducible.
        return List.of(
                new ExternalTransaction("BANK_ALPHA", "A-" + customerId + "-1", Instant.parse("2026-11-10T09:00:00Z"), new BigDecimal("-120.00"), "ZAR", "Grocery shop", "FRESH MARKET"),
                new ExternalTransaction("BANK_ALPHA", "A-" + customerId + "-2", Instant.parse("2026-11-10T09:02:00Z"), new BigDecimal("-15000.00"), "ZAR", "Crypto cash out", "CRYPTO-CASHOUT"),
                new ExternalTransaction("BANK_ALPHA", "A-" + customerId + "-3", Instant.parse("2026-11-10T09:03:00Z"), new BigDecimal("-60.00"), "ZAR", "Fuel station", "SHELL FUEL"),
                new ExternalTransaction("BANK_BETA", "B-" + customerId + "-1", Instant.parse("2026-11-10T09:05:00Z"), new BigDecimal("-90.00"), "ZAR", "Cafe lunch", "CAFE CENTRAL"),
                new ExternalTransaction("BANK_BETA", "B-" + customerId + "-2", Instant.parse("2026-11-11T08:00:00Z"), new BigDecimal("25000.00"), "ZAR", "Salary deposit", "EMPLOYER"),
                new ExternalTransaction("BANK_BETA", "B-" + customerId + "-3", Instant.parse("2026-11-12T11:00:00Z"), new BigDecimal("-900.00"), "ZAR", "Online shopping", "ONLINE STORE")
        );
    }
}
