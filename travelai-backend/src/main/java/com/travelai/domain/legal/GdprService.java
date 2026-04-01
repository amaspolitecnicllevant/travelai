package com.travelai.domain.legal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.travelai.domain.auth.RefreshTokenRepository;
import com.travelai.domain.auth.User;
import com.travelai.domain.auth.UserRepository;
import com.travelai.domain.legal.dto.UserDataExportDto;
import com.travelai.domain.trip.*;
import com.travelai.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * GDPR service — dret d'accés (Art. 15), portabilitat (Art. 20) i dret a l'oblit (Art. 17).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GdprService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ItineraryRepository itineraryRepository;
    private final RatingRepository ratingRepository;
    private final ConsentLogRepository consentLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final DataDeletionRequestRepository deletionRequestRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    // ── Data export (Art. 15 + Art. 20) ──────────────────────────────────────

    /**
     * Exports all personal data for the given user as a ZIP byte array
     * containing a single JSON file.
     *
     * @param userId the UUID of the authenticated user
     * @return ZIP bytes ready to stream as an HTTP response
     */
    @Transactional(readOnly = true)
    public byte[] exportUserData(UUID userId) {
        User user = findUserOrThrow(userId);

        // Profile
        UserDataExportDto.Profile profile = new UserDataExportDto.Profile(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            null,  // name field not present in current User entity
            null,  // bio
            null,  // avatarUrl
            false, // ageVerified maps to age_verified column (not in current entity)
            user.getCreatedAt()
        );

        // Trips
        List<Trip> trips = tripRepository.findByOwnerAndDeletedAtNull(user);
        List<Map<String, Object>> tripsData = trips.stream()
            .map(t -> Map.<String, Object>of(
                "id", t.getId(),
                "title", t.getTitle(),
                "destination", t.getDestination(),
                "visibility", t.getVisibility().name(),
                "status", t.getStatus().name(),
                "startDate", t.getStartDate() != null ? t.getStartDate().toString() : "",
                "endDate", t.getEndDate() != null ? t.getEndDate().toString() : "",
                "createdAt", t.getCreatedAt()
            ))
            .toList();

        // Itineraries
        List<Map<String, Object>> itinerariesData = trips.stream()
            .flatMap(t -> itineraryRepository.findByTripOrderByDayNumber(t).stream())
            .map(i -> Map.<String, Object>of(
                "tripId", i.getTrip().getId(),
                "dayNumber", i.getDayNumber(),
                "date", i.getDate() != null ? i.getDate().toString() : "",
                "contentJson", i.getContentJson(),
                "generatedByAi", i.isGeneratedByAi()
            ))
            .toList();

        // Ratings
        List<Map<String, Object>> ratingsData = trips.stream()
            .flatMap(t -> ratingRepository.findByTrip(t).stream())
            .map(r -> Map.<String, Object>of(
                "tripId", r.getTrip().getId(),
                "score", r.getScore(),
                "comment", r.getComment() != null ? r.getComment() : "",
                "createdAt", r.getCreatedAt()
            ))
            .toList();

        // Consents (without IPs for GDPR minimisation)
        List<UserDataExportDto.ConsentEntry> consents =
            consentLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(c -> new UserDataExportDto.ConsentEntry(
                    c.getConsentType(),
                    c.getConsentVersion(),
                    c.isAccepted(),
                    c.getCreatedAt()
                ))
                .toList();

        // Audit logs (entity / entityId only, no IPs)
        List<UserDataExportDto.AuditEntry> auditEntries =
            auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(a -> new UserDataExportDto.AuditEntry(
                    a.getAction(),
                    a.getEntity(),
                    a.getEntityId(),
                    a.getCreatedAt()
                ))
                .toList();

        UserDataExportDto exportDto = new UserDataExportDto(
            profile,
            tripsData,
            itinerariesData,
            ratingsData,
            consents,
            auditEntries,
            Instant.now()
        );

        byte[] jsonBytes = toJsonBytes(exportDto);
        byte[] zipBytes = wrapInZip("user-data-export.json", jsonBytes);

        auditService.log(userId.toString(), "DATA_EXPORT", "user", userId.toString());

        return zipBytes;
    }

    // ── Deletion request (Art. 17) ─────────────────────────────────────────────

    /**
     * Creates a pending deletion request scheduled 30 days from now.
     * Deactivates the account immediately and revokes all refresh tokens.
     */
    @Transactional
    public DataDeletionRequest requestDeletion(UUID userId) {
        User user = findUserOrThrow(userId);

        if (deletionRequestRepository.existsByUserIdAndStatus(userId, DataDeletionRequest.DeletionStatus.PENDING)) {
            throw new IllegalStateException("Ja existeix una sol·licitud d'esborrat pendent per a aquest compte");
        }

        // Deactivate account immediately
        user.setActive(false);
        userRepository.save(user);

        // Revoke all active sessions
        refreshTokenRepository.revokeAllByUserId(userId);

        DataDeletionRequest request = DataDeletionRequest.builder()
            .user(user)
            .build(); // PrePersist sets requestedAt and scheduledPurgeAt = +30 days

        DataDeletionRequest saved = deletionRequestRepository.save(request);

        auditService.log(userId.toString(), "DELETE_REQUEST", "user", userId.toString(),
            "scheduledFor=" + saved.getScheduledPurgeAt());

        log.info("Deletion request created for user {} — scheduled for {}", userId, saved.getScheduledPurgeAt());
        return saved;
    }

    /**
     * Cancels a pending deletion request and reactivates the account.
     */
    @Transactional
    public void cancelDeletion(UUID userId) {
        User user = findUserOrThrow(userId);

        DataDeletionRequest pending = deletionRequestRepository
            .findByUserIdAndStatus(userId, DataDeletionRequest.DeletionStatus.PENDING)
            .orElseThrow(() -> new ResourceNotFoundException("DELETION_NOT_FOUND",
                "No hi ha cap sol·licitud d'esborrat pendent"));

        deletionRequestRepository.delete(pending);

        user.setActive(true);
        userRepository.save(user);

        auditService.log(userId.toString(), "DELETE_REQUEST_CANCELLED", "user", userId.toString());

        log.info("Deletion request cancelled for user {}", userId);
    }

    /**
     * Returns the full consent history for the given user.
     */
    @Transactional(readOnly = true)
    public List<ConsentLog> getConsentHistory(UUID userId) {
        return consentLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuari no trobat"));
    }

    private byte[] toJsonBytes(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.writeValueAsBytes(obj);
        } catch (IOException e) {
            throw new RuntimeException("Error serialitzant dades a JSON", e);
        }
    }

    private byte[] wrapInZip(String fileName, byte[] content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error creant ZIP d'exportació", e);
        }
    }
}
