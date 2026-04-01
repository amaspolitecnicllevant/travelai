package com.travelai.domain.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

    /** Returns the active document for the given type (slug maps to uppercase type). */
    Optional<LegalDocument> findByTypeAndActiveTrue(String type);
}
