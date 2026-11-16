
package com.banking.platform.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for importing, retrieving, and aggregating customer transactions.
 */
@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final MockBankSource mock;
    private final CategoryService categories;

    /**
     * Initializes the transaction service with required dependencies.
     *
     * @param repository transaction repository
     * @param mock mock banking data source
     * @param categories transaction categorization service
     */
    public TransactionService(
            TransactionRepository repository,
            MockBankSource mock,
            CategoryService categories) {

        this.repository = repository;
        this.mock = mock;
        this.categories = categories;
    }

    /**
     * Imports transactions from mock banks without duplicating existing records.
     *
     * @param customerId customer identifier
     * @return number of newly imported transactions
     */
    @Transactional
    public int sync(String customerId) {
        int imported = 0;

        for (var src : mock.fetch(customerId)) {
            if (repository.existsByCustomerIdAndSourceAndSourceId(
                    customerId, src.source(), src.externalId())) {
                continue;
            }

            repository.save(new Transaction(
                    customerId,
                    src.source(),
                    src.externalId(),
                    src.at(),
                    src.amount(),
                    src.currency(),
                    src.description(),
                    src.merchant(),
                    categories.classify(src.description(), src.merchant())
            ));

            imported++;
        }

        return imported;
    }

    /**
     * Retrieves paginated customer transactions within a date range.
     *
     * @param customerId customer identifier
     * @param from start date and time
     * @param to end date and time
     * @param page page number
     * @param size number of records per page
     * @return paginated transactions
     */
    @Transactional(readOnly = true)
    public Page<Transaction> list(
            String customerId,
            Instant from,
            Instant to,
            int page,
            int size) {

        validateRange(from, to);

        return repository.findByCustomerIdAndOccurredAtBetween(
                customerId,
                from,
                to,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "occurredAt")
                )
        );
    }

    /**
     * Retrieves a transaction belonging to a specific customer.
     *
     * @param customerId customer identifier
     * @param id transaction identifier
     * @return matching transaction
     */
    @Transactional(readOnly = true)
    public Transaction find(String customerId, UUID id) {
        return repository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found"
                ));
    }

    /**
     * Validates the transaction date range.
     *
     * @param from start date and time
     * @param to end date and time
     */
    public void validateRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "from must not exceed to"
            );
        }
    }

    /**
     * Calculates income, expenses, and category totals for a customer.
     *
     * @param customerId customer identifier
     * @param from start date and time
     * @param to end date and time
     * @param currency transaction currency
     * @return aggregated transaction summary
     */
    @Transactional(readOnly = true)
    public Summary summary(
            String customerId,
            Instant from,
            Instant to,
            String currency) {

        validateRange(from, to);

        var transactions = repository
                .findByCustomerIdAndOccurredAtBetween(customerId, from, to)
                .stream()
                .filter(t -> t.currency.equalsIgnoreCase(currency))
                .toList();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        Map<String, BigDecimal> categories = new TreeMap<>();

        for (var t : transactions) {
            if (t.amount.signum() >= 0) {
                income = income.add(t.amount);
            } else {
                BigDecimal expense = t.amount.abs();
                expenses = expenses.add(expense);
                categories.merge(t.category, expense, BigDecimal::add);
            }
        }

        return new Summary(
                customerId,
                currency.toUpperCase(Locale.ROOT),
                income,
                expenses,
                income.subtract(expenses),
                transactions.size(),
                categories
        );
    }

    /**
     * Represents aggregated financial transaction details.
     *
     * @param customerId customer identifier
     * @param currency transaction currency
     * @param income total credited amount
     * @param expenses total debited amount
     * @param netCashFlow income minus expenses
     * @param transactionCount number of transactions
     * @param spendingByCategory expenses grouped by category
     */
    public record Summary(
            String customerId,
            String currency,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal netCashFlow,
            int transactionCount,
            Map<String, BigDecimal> spendingByCategory) {
    }
}
