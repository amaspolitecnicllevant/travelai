package com.travelai.domain.legal.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exportació completa de dades personals (Art. 15 + Art. 20 RGPD).
 * Lliurat com a JSON via GET /api/my-data/export.
 *
 * Les col·leccions de viatges, itineraris i valoracions s'exposen com
 * Map<String, Object> per evitar acoblament entre dominis en temps de
 * compilació; el GdprService les omple via JdbcTemplate.
 */
public record UserDataExportDto(

    Profile profile,
    List<Map<String, Object>> trips,
    List<Map<String, Object>> itineraries,
    List<Map<String, Object>> ratings,
    List<ConsentEntry> consents,
    List<AuditEntry> auditLogs,
    Instant exportedAt

) {
    public record Profile(
        UUID id,
        String username,
        String email,
        String name,
        String bio,
        String avatarUrl,
        boolean ageVerified,
        Instant createdAt
    ) {}

    public record ConsentEntry(
        String consentType,
        String consentVersion,
        boolean accepted,
        Instant createdAt
    ) {}

    public record AuditEntry(
        String action,
        String entity,
        UUID entityId,
        Instant createdAt
    ) {}
}
