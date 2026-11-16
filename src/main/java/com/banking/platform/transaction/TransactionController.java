
package com.banking.platform.transaction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

/**
 * REST controller for transaction synchronization, retrieval,
 * and financial aggregation.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {

    private final TransactionService service;

    /**
     * Initializes the transaction controller.
     *
     * @param service transaction management service
     */
    public TransactionController(TransactionService service) {
        this.service = service;
    }

    /**
     * Imports transactions from configured mock banking sources.
     *
     * @param auth authenticated user
     * @return number of imported transactions
     */
    @PostMapping("/sync")
    public Map<String, Integer> sync(Authentication auth) {
        return Map.of("imported", service.sync(auth.getName()));
    }

    /**
     * Retrieves paginated customer transactions within a date range.
     *
     * @param auth authenticated user
     * @param from start date and time
     * @param to end date and time
     * @param page page number, starting from zero
     * @param size maximum records per page
     * @return paginated transaction results
     */
    @GetMapping
    public Page<Transaction> list(
            Authentication auth,
            @RequestParam(defaultValue = "2020-01-01T00:00:00Z") Instant from,
            @RequestParam(defaultValue = "2100-01-01T00:00:00Z") Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return service.list(auth.getName(), from, to, page, size);
    }

    /**
     * Retrieves a specific transaction belonging to the customer.
     *
     * @param auth authenticated user
     * @param id unique transaction identifier
     * @return transaction details
     */
    @GetMapping("/{id}")
    public Transaction get(Authentication auth, @PathVariable UUID id) {
        return service.find(auth.getName(), id);
    }

    /**
     * Retrieves aggregated income, expenses, and spending summaries.
     *
     * @param auth authenticated user
     * @param from start date and time
     * @param to end date and time
     * @param currency three-letter currency code
     * @return transaction summary
     */
    @GetMapping("/summary")
    public TransactionService.Summary summary(
            Authentication auth,
            @RequestParam(defaultValue = "2020-01-01T00:00:00Z") Instant from,
            @RequestParam(defaultValue = "2100-01-01T00:00:00Z") Instant to,
            @RequestParam(defaultValue = "ZAR")
            @Pattern(regexp = "[A-Za-z]{3}") String currency) {

        return service.summary(auth.getName(), from, to, currency);
    }

    /**
     * Retrieves customer spending totals grouped by category.
     *
     * @param auth authenticated user
     * @param from start date and time
     * @param to end date and time
     * @param currency three-letter currency code
     * @return spending totals by transaction category
     */
    @GetMapping("/categories")
    public Map<String, java.math.BigDecimal> categories(
            Authentication auth,
            @RequestParam(defaultValue = "2020-01-01T00:00:00Z") Instant from,
            @RequestParam(defaultValue = "2100-01-01T00:00:00Z") Instant to,
            @RequestParam(defaultValue = "ZAR")
            @Pattern(regexp = "[A-Za-z]{3}") String currency) {

        return service.summary(
                auth.getName(), from, to, currency
        ).spendingByCategory();
    }
}
