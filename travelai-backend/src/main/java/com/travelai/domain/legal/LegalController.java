package com.travelai.domain.legal;

import com.travelai.domain.auth.User;
import com.travelai.domain.legal.dto.DeletionRequestResponse;
import com.travelai.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints for GDPR self-service and legal document retrieval.
 *
 * Public:
 *   GET  /api/v1/legal/{slug}
 *
 * Authenticated (requires valid JWT):
 *   GET    /api/v1/users/me/data-export
 *   POST   /api/v1/users/me/delete-request
 *   DELETE /api/v1/users/me/delete-request
 *   GET    /api/v1/users/me/consent-history
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class LegalController {

    private final LegalDocumentRepository legalDocumentRepository;
    private final GdprService gdprService;

    // ── Legal documents ────────────────────────────────────────────────────────

    /**
     * Returns the active legal document identified by {@code slug}.
     * Slug examples: privacy-policy, terms, cookies, legal-notice.
     * Slug is uppercased and hyphens are replaced with underscores to match DB type column.
     */
    @GetMapping("/api/v1/legal/{slug}")
    public ResponseEntity<Map<String, Object>> getLegalDocument(@PathVariable String slug) {
        String type = slug.toUpperCase().replace("-", "_");

        LegalDocument doc = legalDocumentRepository.findByTypeAndActiveTrue(type)
            .orElseThrow(() -> new ResourceNotFoundException("LEGAL_DOC_NOT_FOUND",
                "Document legal no trobat: " + slug));

        return ResponseEntity.ok(Map.of(
            "type", doc.getType(),
            "version", doc.getVersion(),
            "content", doc.getContent(),
            "publishedAt", doc.getPublishedAt()
        ));
    }

    // ── GDPR self-service ──────────────────────────────────────────────────────

    /**
     * Streams all personal data as a ZIP file (Art. 15 + Art. 20).
     */
    @GetMapping("/api/v1/users/me/data-export")
    public ResponseEntity<byte[]> exportMyData(@AuthenticationPrincipal User user) {
        byte[] zipBytes = gdprService.exportUserData(user.getId());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"travelai-data-export.zip\"")
            .contentType(MediaType.parseMediaType("application/zip"))
            .contentLength(zipBytes.length)
            .body(zipBytes);
    }

    /**
     * Requests account deletion scheduled 30 days from now (Art. 17).
     */
    @PostMapping("/api/v1/users/me/delete-request")
    public ResponseEntity<DeletionRequestResponse> requestDeletion(
            @AuthenticationPrincipal User user) {
        DataDeletionRequest req = gdprService.requestDeletion(user.getId());
        return ResponseEntity.accepted().body(toResponse(req));
    }

    /**
     * Cancels a pending deletion request.
     */
    @DeleteMapping("/api/v1/users/me/delete-request")
    public ResponseEntity<Void> cancelDeletion(@AuthenticationPrincipal User user) {
        gdprService.cancelDeletion(user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the full consent history for the authenticated user.
     */
    @GetMapping("/api/v1/users/me/consent-history")
    public ResponseEntity<List<Map<String, Object>>> getConsentHistory(
            @AuthenticationPrincipal User user) {
        List<Map<String, Object>> history = gdprService.getConsentHistory(user.getId()).stream()
            .map(c -> Map.<String, Object>of(
                "consentType", c.getConsentType(),
                "consentVersion", c.getConsentVersion(),
                "accepted", c.isAccepted(),
                "createdAt", c.getCreatedAt()
            ))
            .toList();
        return ResponseEntity.ok(history);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private DeletionRequestResponse toResponse(DataDeletionRequest req) {
        return new DeletionRequestResponse(
            req.getId(),
            req.getRequestedAt(),
            req.getScheduledPurgeAt(),
            req.getStatus()
        );
    }
}
