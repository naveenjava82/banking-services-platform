
package com.banking.platform.fraud;

import java.util.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for fraud assessment and retrieving fraud findings.
 */
@RestController
@RequestMapping("/api/v1/fraud")
public class FraudController {

    private final FraudService service;

    /**
     * Initializes the fraud controller.
     *
     * @param service fraud assessment service
     */
    public FraudController(FraudService service) {
        this.service = service;
    }

    /**
     * Assesses all transactions for the authenticated customer.
     *
     * @param auth authenticated user
     * @return total number of fraud findings
     */
    @PostMapping("/assess-all")
    public Map<String, Integer> assessAll(Authentication auth) {
        return Map.of("totalFindings", service.assessAll(auth.getName()));
    }

    /**
     * Assesses a specific transaction for potential fraud.
     *
     * @param auth authenticated user
     * @param transactionId unique transaction identifier
     * @return list of fraud findings for the transaction
     */
    @PostMapping("/assess/{transactionId}")
    public List<FraudFinding> assess(
            Authentication auth,
            @PathVariable UUID transactionId) {

        return service.assess(auth.getName(), transactionId);
    }

    /**
     * Retrieves fraud findings for the authenticated customer.
     *
     * @param auth authenticated user
     * @return list of recorded fraud findings
     */
    @GetMapping("/findings")
    public List<FraudFinding> findings(Authentication auth) {
        return service.findings(auth.getName());
    }
}
