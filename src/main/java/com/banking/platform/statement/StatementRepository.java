package com.banking.platform.statement;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementRepository extends JpaRepository<Statement, UUID> {
    Optional<Statement> findByIdAndCustomerId(UUID id, String customerId);

    List<Statement> findByCustomerIdOrderByUploadedAtDesc(String customerId);
}
