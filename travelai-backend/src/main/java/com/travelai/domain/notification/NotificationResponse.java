package com.travelai.domain.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta per a una notificació.
 */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        String entityType,
        UUID entityId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getEntityType(),
                n.getEntityId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
