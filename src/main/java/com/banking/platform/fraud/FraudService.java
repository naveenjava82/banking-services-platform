
package com.banking.platform.fraud;

import com.banking.platform.transaction.*;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for evaluating transactions against fraud detection rules
 * and managing fraud findings.
 */
@Service
public class FraudService {

    private final FraudRepository findings;
    private final TransactionRepository transactions;
    private final BigDecimal threshold;
    private final int count;
    private final long windowMinutes;
    private final Set<String> risky;

    /**
     * Initializes the fraud service with repositories and rule configurations.
     *
     * @param findings fraud findings repository
     * @param transactions transaction repository
     * @param threshold high-value debit threshold
     * @param count minimum transaction count for velocity detection
     * @param windowMinutes time window for velocity detection
     * @param merchants comma-separated list of high-risk merchants
     */
    public FraudService(
            FraudRepository findings,
            TransactionRepository transactions,
            @Value("${app.fraud.amount-threshold}") BigDecimal threshold,
            @Value("${app.fraud.velocity-count}") int count,
            @Value("${app.fraud.velocity-window-minutes}") long windowMinutes,
            @Value("${app.fraud.high-risk-merchants}") String merchants) {

        this.findings = findings;
        this.transactions = transactions;
        this.threshold = threshold;
        this.count = count;
        this.windowMinutes = windowMinutes;
        this.risky = new HashSet<>(
                Arrays.asList(
                        merchants.toUpperCase(Locale.ROOT).split(",")
                )
        );
    }

    /**
     * Evaluates a transaction against configured fraud rules.
     *
     * @param t transaction to evaluate
     * @return list of matching fraud rules
     */
    public List<RuleMatch> evaluate(Transaction t) {
        List<RuleMatch> matches = new ArrayList<>();

        if (t.amount.signum() < 0
                && t.amount.abs().compareTo(threshold) > 0) {
            matches.add(new RuleMatch(
                    "HIGH_AMOUNT",
                    "Debit exceeds configured threshold"));
        }

        if (t.amount.signum() < 0
                && risky.contains(t.merchant.toUpperCase(Locale.ROOT))) {
            matches.add(new RuleMatch(
                    "HIGH_RISK_MERCHANT",
                    "Merchant matches configured risk list"));
        }

        if (t.amount.signum() < 0
                && transactions.countByCustomerIdAndOccurredAtBetweenAndAmountLessThan(
                t.customerId,
                t.occurredAt.minusSeconds(windowMinutes * 60),
                t.occurredAt,
                BigDecimal.ZERO) >= count) {
            matches.add(new RuleMatch(
                    "HIGH_VELOCITY",
                    "Too many debit transactions in configured window"));
        }

        return matches;
    }

    /**
     * Assesses a customer transaction and stores new fraud findings.
     *
     * @param customerId customer identifier
     * @param transactionId transaction identifier
     * @return fraud findings associated with the transaction
     */
    @Transactional
    public List<FraudFinding> assess(String customerId, UUID transactionId) {
        Transaction transaction = transactions
                .findByIdAndCustomerId(transactionId, customerId)
                .orElseThrow(() ->
                        new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "Transaction not found"));

        for (var match : evaluate(transaction)) {
            if (!findings.existsByTransactionIdAndRuleCode(
                    transactionId, match.code())) {
                findings.save(new FraudFinding(
                        transactionId,
                        customerId,
                        match.code(),
                        match.reason()));
            }
        }

        return findings.findByCustomerIdAndTransactionId(
                customerId, transactionId);
    }

    /**
     * Assesses all transactions belonging to a customer.
     *
     * @param customerId customer identifier
     * @return total number of recorded fraud findings for the customer
     */
    @Transactional
    public int assessAll(String customerId) {
        var records = transactions.findByCustomerIdAndOccurredAtBetween(
                customerId,
                java.time.Instant.parse("2020-01-01T00:00:00Z"),
                java.time.Instant.parse("2100-01-01T00:00:00Z"));

        for (var t : records) {
            assess(customerId, t.id);
        }

        return findings
                .findByCustomerIdOrderByDetectedAtDesc(customerId)
                .size();
    }

    /**
     * Retrieves recorded fraud findings for a customer.
     *
     * @param customerId customer identifier
     * @return fraud findings ordered by detection time
     */
    @Transactional(readOnly = true)
    public List<FraudFinding> findings(String customerId) {
        return findings.findByCustomerIdOrderByDetectedAtDesc(customerId);
    }

    /**
     * Represents a matched fraud rule and its detection reason.
     *
     * @param code fraud rule identifier
     * @param reason reason the rule was triggered
     */
    public record RuleMatch(String code, String reason) {
    }
}
