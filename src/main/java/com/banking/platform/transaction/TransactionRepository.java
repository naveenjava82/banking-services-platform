package com.banking.platform.transaction;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    boolean existsByCustomerIdAndSourceAndSourceId(String customerId, String source, String sourceId);

    Optional<Transaction> findByIdAndCustomerId(UUID id, String customerId);

    Page<Transaction> findByCustomerIdAndOccurredAtBetween(String customerId, Instant from, Instant to, Pageable pageable);

    java.util.List<Transaction> findByCustomerIdAndOccurredAtBetween(String customerId, Instant from, Instant to);

    long countByCustomerIdAndOccurredAtBetweenAndAmountLessThan(String customerId, Instant from, Instant to, java.math.BigDecimal amount);
}
