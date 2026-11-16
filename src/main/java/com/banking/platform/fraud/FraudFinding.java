
package com.banking.platform.fraud;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a fraud finding for a customer transaction.
 */
@Entity
@Table(
        name = "fraud_findings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"transaction_id", "rule_code"}
        )
)
public class FraudFinding {

    @Id
    public UUID id;

    @Column(name = "transaction_id", nullable = false)
    public UUID transactionId;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "rule_code", nullable = false)
    public String ruleCode;

    @Column(nullable = false)
    public String reason;

    @Column(name = "detected_at", nullable = false)
    public Instant detectedAt;

    /**
     * Default constructor required by JPA.
     */
    protected FraudFinding() {
    }

    /**
     * Creates a fraud finding for a transaction and rule.
     *
     * @param transactionId unique transaction identifier
     * @param customerId customer identifier
     * @param ruleCode fraud detection rule code
     * @param reason description of the detected fraud condition
     */
    public FraudFinding(
            UUID transactionId,
            String customerId,
            String ruleCode,
            String reason) {

        id = UUID.randomUUID();
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.ruleCode = ruleCode;
        this.reason = reason;
        detectedAt = Instant.now();
    }
}
