package com.travelai.domain.legal;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Pot ser null si el compte s'ha esborrat (ON DELETE SET NULL) */
    @Column(name = "user_id")
    private UUID userId;

    /** Acció realitzada: USER_LOGIN, DATA_EXPORT, DELETE_REQUEST, etc. */
    @Column(nullable = false, length = 100)
    private String action;

    /** Tipus d'entitat afectada (user, trip, …) */
    @Column(length = 50)
    private String entity;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
