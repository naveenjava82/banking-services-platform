
package com.banking.platform.transaction;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a customer banking transaction.
 */
@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"customer_id", "source", "source_id"}
        )
)
public class Transaction {

    @Id
    public UUID id;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(nullable = false)
    public String source;

    @Column(name = "source_id", nullable = false)
    public String sourceId;

    @Column(name = "occurred_at", nullable = false)
    public Instant occurredAt;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false, length = 3)
    public String currency;

    @Column(nullable = false, length = 500)
    public String description;

    @Column(nullable = false)
    public String merchant;

    @Column(nullable = false)
    public String category;

    /**
     * Default constructor required by JPA.
     */
    protected Transaction() {
    }

    /**
     * Creates a new customer transaction with a unique identifier.
     *
     * @param customerId customer identifier
     * @param source transaction source bank
     * @param sourceId transaction identifier from the source bank
     * @param occurredAt transaction date and time
     * @param amount transaction amount
     * @param currency transaction currency
     * @param description transaction description
     * @param merchant merchant name
     * @param category transaction category
     */
    public Transaction(
            String customerId,
            String source,
            String sourceId,
            Instant occurredAt,
            BigDecimal amount,
            String currency,
            String description,
            String merchant,
            String category) {

        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.source = source;
        this.sourceId = sourceId;
        this.occurredAt = occurredAt;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.merchant = merchant;
        this.category = category;
    }
}
