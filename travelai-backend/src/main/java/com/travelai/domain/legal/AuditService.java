package com.travelai.domain.legal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servei d'auditoria per a accions sensibles (RGPD Art. 5.2 — responsabilitat).
 * Les crides són asíncrones per no bloquejar el fil principal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Persists a sensitive action with full detail.
     *
     * @param userId     user performing the action (may be null for anonymous events)
     * @param action     action code, e.g. DATA_EXPORT, DELETE_REQUEST, TRIP_PUBLISHED
     * @param entityType type of entity affected, e.g. "user", "trip"
     * @param entityId   affected entity id (as String, may be a UUID or any identifier)
     * @param detail     free-text or JSON detail attached to the log entry
     */
    @Async
    public void log(String userId, String action, String entityType, String entityId, String detail) {
        try {
            AuditLog entry = AuditLog.builder()
                .userId(userId != null ? UUID.fromString(userId) : null)
                .action(action)
                .entity(entityType)
                .entityId(entityId != null ? parseUuidOrNull(entityId) : null)
                .details(detail != null ? java.util.Map.of("detail", detail) : null)
                .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Error persisting audit log [{}/{}]: {}", action, entityType, ex.getMessage());
        }
    }

    /**
     * Persists a sensitive action without extra detail.
     */
    @Async
    public void log(String userId, String action, String entityType, String entityId) {
        log(userId, action, entityType, entityId, null);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
