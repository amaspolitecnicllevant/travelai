package com.travelai.domain.legal;

import com.travelai.domain.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deletion_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataDeletionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    /** Data en la qual s'executarà la purga irreversible (NOW() + 30 dies) */
    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledPurgeAt;

    /** Moment real d'execució de la purga */
    @Column(name = "completed_at")
    private Instant executedAt;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeletionStatus status = DeletionStatus.PENDING;

    @PrePersist
    void prePersist() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (scheduledPurgeAt == null) scheduledPurgeAt = requestedAt.plusSeconds(30L * 24 * 3600);
    }

    public enum DeletionStatus {
        PENDING, EXECUTED
    }
}
