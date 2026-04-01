package com.travelai.domain.legal;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legal_documents",
    uniqueConstraints = @UniqueConstraint(columnNames = {"type", "version"}),
    indexes = @Index(name = "idx_legal_documents_type_active", columnList = "type, active"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Document type: PRIVACY_POLICY, TERMS, COOKIES, LEGAL_NOTICE */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @PrePersist
    void prePersist() {
        if (publishedAt == null) publishedAt = Instant.now();
    }
}
