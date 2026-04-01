package com.travelai.domain.legal;

import com.travelai.domain.auth.UserRepository;
import com.travelai.domain.trip.TripRepository;
import com.travelai.domain.trip.Visibility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Nightly scheduler that permanently anonymizes accounts with due deletion requests.
 * Runs at 02:00 every night (server time) — GDPR Art. 17, right to erasure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeletionScheduler {

    private final DataDeletionRequestRepository deletionRequestRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processDueDeletions() {
        List<DataDeletionRequest> due = deletionRequestRepository.findDuePurges(Instant.now());

        if (due.isEmpty()) {
            log.debug("DeletionScheduler — no pending deletions due");
            return;
        }

        log.info("DeletionScheduler — processing {} deletion request(s)", due.size());

        for (DataDeletionRequest request : due) {
            try {
                executeAnonymisation(request);
            } catch (Exception ex) {
                log.error("DeletionScheduler — failed to process deletion request {} for user {}: {}",
                    request.getId(), request.getUser().getId(), ex.getMessage(), ex);
            }
        }
    }

    // ── private ────────────────────────────────────────────────────────────────

    private void executeAnonymisation(DataDeletionRequest request) {
        UUID userId = request.getUser().getId();
        log.info("Anonymising account {} (deletion request {})", userId, request.getId());

        userRepository.findById(userId).ifPresent(user -> {
            // Anonymise personal data
            user.setEmail(UUID.randomUUID() + "@deleted.local");
            user.setUsername("deleted_" + UUID.randomUUID().toString().substring(0, 8));
            user.setPasswordHash("DELETED");
            user.setActive(false);
            user.setDeletedAt(Instant.now());
            userRepository.save(user);

            // Delete private trips owned by this user
            tripRepository.findByOwnerAndDeletedAtNull(user).stream()
                .filter(t -> t.getVisibility() == Visibility.PRIVATE)
                .forEach(t -> {
                    t.setDeletedAt(Instant.now());
                    tripRepository.save(t);
                });
        });

        // Mark deletion request as executed
        request.setStatus(DataDeletionRequest.DeletionStatus.EXECUTED);
        request.setExecutedAt(Instant.now());
        deletionRequestRepository.save(request);

        // Async audit — fire and forget
        auditService.log(userId.toString(), "ACCOUNT_PURGED", "user", userId.toString(),
            "deletionRequestId=" + request.getId());

        log.info("Account {} anonymised successfully", userId);
    }
}
