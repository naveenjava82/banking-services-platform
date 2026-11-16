
package com.banking.platform.statement;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a customer's uploaded PDF bank statement.
 */
@Entity
@Table(name = "statements")
public class Statement {

    @Id
    public UUID id;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "original_filename", nullable = false)
    public String originalFilename;

    @Column(name = "storage_filename", nullable = false, unique = true)
    public String storageFilename;

    @Column(name = "uploaded_at", nullable = false)
    public Instant uploadedAt;

    @Column(name = "file_size", nullable = false)
    public long fileSize;

    @Column(nullable = false)
    public String sha256;

    /**
     * Default constructor required by JPA.
     */
    protected Statement() {
    }

    /**
     * Creates a new statement with file metadata and a unique identifier.
     *
     * @param customerId customer identifier
     * @param originalFilename original uploaded PDF filename
     * @param storageFilename filename used for secure storage
     * @param fileSize size of the uploaded file in bytes
     * @param sha256 SHA-256 hash of the file contents
     */
    public Statement(
            String customerId,
            String originalFilename,
            String storageFilename,
            long fileSize,
            String sha256) {

        id = UUID.randomUUID();
        this.customerId = customerId;
        this.originalFilename = originalFilename;
        this.storageFilename = storageFilename;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        uploadedAt = Instant.now();
    }
}
