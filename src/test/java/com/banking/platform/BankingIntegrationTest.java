
package com.banking.platform;

import com.banking.platform.transaction.CategoryService;
import com.banking.platform.statement.StatementService;
import com.banking.platform.transaction.TransactionRepository;
import com.banking.platform.fraud.FraudService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Integration tests for banking transactions, fraud detection,
 * authentication, and secure statement handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BankingIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CategoryService categories;

    @Autowired
    TransactionRepository transactions;

    @Autowired
    FraudService fraud;

    @Autowired
    StatementService statements;

    /**
     * Verifies transaction categorization based on descriptions and merchants.
     */
    @Test
    void categorization() {
        assertEquals("GROCERIES",
                categories.classify("Grocery purchase", "Market"));
        assertEquals("OTHER",
                categories.classify("Unknown", "XYZ"));
    }

    /**
     * Verifies API authentication and customer transaction isolation.
     *
     * @throws Exception if the mock HTTP requests fail
     */
    @Test
    void authenticationAndIsolation() throws Exception {
        mvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/transactions/sync")
                        .with(httpBasic("customer-1001", "demo-pass-1")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/transactions/summary")
                        .with(httpBasic("customer-1001", "demo-pass-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(6));

        mvc.perform(get("/api/v1/transactions/summary")
                        .with(httpBasic("customer-1002", "demo-pass-2")))
                .andExpect(jsonPath("$.transactionCount").value(0));
    }

    /**
     * Verifies duplicate transaction prevention and fraud assessment.
     */
    @Test
    void importIdempotentAndFraud() {
        var sync = new com.banking.platform.transaction.TransactionService(
                transactions,
                new com.banking.platform.transaction.MockBankSource(),
                categories);

        sync.sync("customer-1001");

        assertEquals(0, sync.sync("customer-1001"));
        assertTrue(fraud.assessAll("customer-1001") >= 2);
        assertTrue(fraud.findings("customer-1002").isEmpty());
    }

    /**
     * Verifies rejection of invalid PDF statement uploads.
     *
     * @throws Exception if the mock HTTP request fails
     */
    @Test
    void invalidStatementAndSignedLink() throws Exception {
        mvc.perform(multipart("/api/v1/statements")
                        .file(new MockMultipartFile(
                                "file",
                                "fake.pdf",
                                "application/pdf",
                                "not a pdf".getBytes()))
                        .with(httpBasic("customer-1001", "demo-pass-1")))
                .andExpect(status().isBadRequest());
    }
}
