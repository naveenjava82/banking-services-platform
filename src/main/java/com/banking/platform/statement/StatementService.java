
package com.banking.platform.statement;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for secure PDF statement storage, retrieval, and download.
 */
@Service
public class StatementService {

    private final StatementRepository repository;
    private final Path directory;
    private final byte[] secret;
    private final long maxTtl;

    /**
     * Initializes the statement service with storage and security settings.
     *
     * @param repository statement repository
     * @param directory PDF storage directory
     * @param secret HMAC signing secret
     * @param maxTtl maximum download link validity in seconds
     */
    public StatementService(
            StatementRepository repository,
            @Value("${app.statements.directory}") String directory,
            @Value("${app.statements.signing-secret}") String secret,
            @Value("${app.statements.max-ttl-seconds}") long maxTtl) {

        this.repository = repository;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.secret = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        this.maxTtl = maxTtl;

        if (this.secret.length < 32)
            throw new IllegalArgumentException(
                    "Statement signing secret must be at least 32 bytes");
    }

    /**
     * Validates and stores an uploaded PDF statement.
     *
     * @param customerId customer identifier
     * @param file uploaded PDF file
     * @return saved statement metadata
     */
    public Statement upload(String customerId, MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "PDF must be between 1 byte and 10 MB");

        try {
            byte[] bytes = file.getBytes();

            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P'
                    || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-')
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "File is not a PDF");

            try (var doc = Loader.loadPDF(bytes)) {
                if (doc.getNumberOfPages() < 1)
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "PDF has no pages");
            } catch (IOException invalidPdf) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Malformed PDF");
            }

            Files.createDirectories(directory);
            String name = UUID.randomUUID() + ".pdf";
            Path target = directory.resolve(name);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);

            String original = Optional.ofNullable(file.getOriginalFilename())
                    .orElse("statement.pdf")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");

            if (original.length() > 200)
                original = original.substring(original.length() - 200);

            Statement statement = new Statement(
                    customerId, original, name, bytes.length, sha256(bytes));

            try {
                return repository.save(statement);
            } catch (RuntimeException e) {
                Files.deleteIfExists(target);
                throw e;
            }
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store PDF");
        }
    }

    /**
     * Retrieves all statements belonging to a customer.
     *
     * @param customerId customer identifier
     * @return list of customer statements
     */
    public List<Statement> list(String customerId) {
        return repository.findByCustomerIdOrderByUploadedAtDesc(customerId);
    }

    /**
     * Retrieves statement metadata for a specific customer.
     *
     * @param customerId customer identifier
     * @param id statement identifier
     * @return statement metadata
     */
    public Statement get(String customerId, UUID id) {
        return repository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Statement not found"));
    }

    /**
     * Generates a signed, time-limited statement download link.
     *
     * @param customerId customer identifier
     * @param id statement identifier
     * @param ttl link validity duration in seconds
     * @return signed download link and expiration time
     */
    public SignedLink createLink(String customerId, UUID id, long ttl) {
        get(customerId, id);

        if (ttl < 1 || ttl > maxTtl)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ttlSeconds must be between 1 and " + maxTtl);

        long expiry = Instant.now().getEpochSecond() + ttl;
        String signature = sign(customerId, id, expiry);

        return new SignedLink(
                "/api/v1/statements/" + id
                        + "/download?expires=" + expiry
                        + "&signature=" + signature,
                Instant.ofEpochSecond(expiry));
    }

    /**
     * Validates a signed download request and retrieves the PDF contents.
     *
     * @param customerId customer identifier
     * @param id statement identifier
     * @param expiry download link expiration timestamp
     * @param signature HMAC signature
     * @return PDF file contents
     */
    public byte[] download(
            String customerId, UUID id, long expiry, String signature) {

        if (expiry <= Instant.now().getEpochSecond())
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Download link expired");

        if (expiry > Instant.now().plusSeconds(maxTtl).getEpochSecond())
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Invalid download expiry");

        byte[] expected = sign(customerId, id, expiry)
                .getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        byte[] actual = signature
                .getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        if (!MessageDigest.isEqual(expected, actual))
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Invalid signature");

        Statement statement = get(customerId, id);

        try {
            byte[] result = Files.readAllBytes(
                    directory.resolve(statement.storageFilename));

            if (!sha256(result).equals(statement.sha256))
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Statement integrity check failed");

            return result;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Stored statement not found");
        }
    }

    /**
     * Generates an HMAC-SHA256 signature for a statement download.
     *
     * @param customerId customer identifier
     * @param id statement identifier
     * @param expiry link expiration timestamp
     * @return URL-safe encoded signature
     */
    private String sign(String customerId, UUID id, long expiry) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));

            byte[] signature = mac.doFinal(
                    (customerId + ":" + id + ":" + expiry)
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(signature);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC is unavailable", e);
        }
    }

    /**
     * Calculates the SHA-256 hash of file contents.
     *
     * @param data file contents
     * @return hexadecimal SHA-256 hash
     */
    private String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(data);

            return HexFormat.of().formatHex(digest);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Represents a signed statement download link.
     *
     * @param downloadPath signed download URL path
     * @param expiresAt link expiration time
     */
    public record SignedLink(String downloadPath, Instant expiresAt) {
    }
}
