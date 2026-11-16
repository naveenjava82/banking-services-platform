
package com.banking.platform.statement;

import java.util.*;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing customer PDF statements
 * and secure statement downloads.
 */
@RestController
@RequestMapping("/api/v1/statements")
public class StatementController {

    private final StatementService service;

    /**
     * Initializes the statement controller.
     *
     * @param service statement management service
     */
    public StatementController(StatementService service) {
        this.service = service;
    }

    /**
     * Uploads a PDF statement for the authenticated customer.
     *
     * @param auth authenticated user
     * @param file PDF file to upload
     * @return uploaded statement metadata
     */
    @PostMapping(path = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Statement upload(
            Authentication auth,
            @RequestPart("file") MultipartFile file) {

        return service.upload(auth.getName(), file);
    }

    /**
     * Retrieves all statements belonging to the authenticated customer.
     *
     * @param auth authenticated user
     * @return list of customer statements
     */
    @GetMapping
    public List<Statement> list(Authentication auth) {
        return service.list(auth.getName());
    }

    /**
     * Retrieves a specific customer's statement metadata.
     *
     * @param auth authenticated user
     * @param id unique statement identifier
     * @return statement metadata
     */
    @GetMapping("/{id}")
    public Statement find(
            Authentication auth,
            @PathVariable UUID id) {

        return service.get(auth.getName(), id);
    }

    /**
     * Generates a time-limited secure download link for a statement.
     *
     * @param auth authenticated user
     * @param id unique statement identifier
     * @param ttlSeconds link validity duration in seconds
     * @return signed statement download link
     */
    @PostMapping("/{id}/download-link")
    public StatementService.SignedLink link(
            Authentication auth,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "300") long ttlSeconds) {

        return service.createLink(auth.getName(), id, ttlSeconds);
    }

    /**
     * Downloads a PDF statement after validating its signed link.
     *
     * @param auth authenticated user
     * @param id unique statement identifier
     * @param expires link expiration timestamp
     * @param signature HMAC signature for download verification
     * @return PDF file as a downloadable HTTP response
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            Authentication auth,
            @PathVariable UUID id,
            @RequestParam long expires,
            @RequestParam String signature) {

        Statement metadata = service.get(auth.getName(), id);

        byte[] bytes = service.download(
                auth.getName(), id, expires, signature);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(metadata.originalFilename)
                                .build()
                                .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(bytes);
    }
}
