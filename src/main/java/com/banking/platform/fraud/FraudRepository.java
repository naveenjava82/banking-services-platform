
package com.banking.platform.fraud;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing fraud findings in the database.
 */
public interface FraudRepository extends JpaRepository<FraudFinding, UUID> {

    /**
     * Retrieves fraud findings for a customer, ordered by detection time.
     *
     * @param customerId customer identifier
     * @return list of fraud findings in descending order
     */
    List<FraudFinding> findByCustomerIdOrderByDetectedAtDesc(String customerId);

    /**
     * Retrieves fraud findings for a specific customer transaction.
     *
     * @param customerId customer identifier
     * @param transactionId unique transaction identifier
     * @return list of matching fraud findings
     */
    List<FraudFinding> findByCustomerIdAndTransactionId(
            String customerId, UUID transactionId);

    /**
     * Checks whether a fraud finding exists for a transaction and rule.
     *
     * @param transactionId unique transaction identifier
     * @param ruleCode fraud detection rule code
     * @return true if a matching fraud finding exists, otherwise false
     */
    boolean existsByTransactionIdAndRuleCode(
            UUID transactionId, String ruleCode);
}
