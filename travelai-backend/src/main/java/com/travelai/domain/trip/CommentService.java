package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import com.travelai.domain.notification.NotificationService;
import com.travelai.domain.notification.NotificationType;
import com.travelai.domain.trip.dto.CommentResponse;
import com.travelai.domain.trip.dto.CreateCommentRequest;
import com.travelai.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository  commentRepository;
    private final TripService        tripService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(UUID tripId, User viewer, Pageable pageable) {
        Trip trip = tripService.findActiveOrThrow(tripId);
        assertCanView(trip, viewer);
        return commentRepository.findByTripId(tripId, pageable).map(CommentResponse::from);
    }

    @Transactional
    public CommentResponse addComment(UUID tripId, CreateCommentRequest req, User author) {
        Trip trip = tripService.findActiveOrThrow(tripId);
        assertCanView(trip, author);

        Comment comment = commentRepository.save(new Comment(trip, author, req.content()));

        // Notify trip owner (not if commenting on own trip)
        if (!trip.getOwner().getId().equals(author.getId())) {
            notificationService.notify(
                trip.getOwner().getId(),
                NotificationType.COMMENT,
                "@" + author.getUsername() + " ha comentat el teu viatge \"" + trip.getTitle() + "\"",
                "trip",
                tripId
            );
        }

        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(UUID tripId, UUID commentId, User requester) {
        Comment comment = commentRepository.findActiveById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("COMMENT_NOT_FOUND", "Comentari no trobat"));

        if (!comment.getTrip().getId().equals(tripId)) {
            throw new ResourceNotFoundException("COMMENT_TRIP_MISMATCH", "Comentari no pertany a aquest viatge");
        }

        boolean isAuthor      = comment.getAuthor().getId().equals(requester.getId());
        boolean isTripOwner   = comment.getTrip().getOwner().getId().equals(requester.getId());
        if (!isAuthor && !isTripOwner) {
            throw new AccessDeniedException("No tens permís per eliminar aquest comentari");
        }

        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertCanView(Trip trip, User viewer) {
        if (trip.getVisibility() == Visibility.PUBLIC) return;
        if (viewer == null) throw new AccessDeniedException("Accés no autoritzat");
        if (trip.getOwner().getId().equals(viewer.getId())) return;
        throw new AccessDeniedException("No tens accés a aquest viatge");
    }
}
