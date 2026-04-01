package com.travelai.domain.notification;

import com.travelai.domain.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NotificationController — endpoints REST per a notificacions.
 *
 * Tots els endpoints requereixen autenticació.
 *
 * Endpoints:
 *   GET /api/v1/notifications          → llista notificacions no llegides
 *   PUT /api/v1/notifications/read-all → marcar totes com a llegides
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Retorna les notificacions no llegides de l'usuari autenticat.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getUnread(user.getId()));
    }

    /**
     * Marca totes les notificacions de l'usuari autenticat com a llegides.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal User user) {
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok().build();
    }
}
