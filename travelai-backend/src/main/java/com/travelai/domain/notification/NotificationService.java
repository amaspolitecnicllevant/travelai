package com.travelai.domain.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * NotificationService — persisteix notificacions i les envia via WebSocket STOMP.
 *
 * El client ha d'estar subscrit a:
 *   /user/queue/notifications
 *
 * SimpMessagingTemplate.convertAndSendToUser() utilitza el userId com a nom d'usuari
 * perquè el HandshakeInterceptor mapeja el principal al UUID de l'usuari.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Persiste una notificació i l'envia en temps real via WebSocket.
     *
     * @param userId  UUID de l'usuari destinatari
     * @param type    Tipus de notificació
     * @param message Missatge llegible per a l'usuari
     */
    @Transactional
    public void notify(UUID userId, NotificationType type, String message) {
        notify(userId, type, message, null, null);
    }

    /**
     * Persiste una notificació amb entitat associada i l'envia via WebSocket.
     */
    @Transactional
    public void notify(UUID userId, NotificationType type, String message,
                       String entityType, UUID entityId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .read(false)
                .build();

        notificationRepository.save(notification);
        log.info("NotificationService: notificació {} creada per a l'usuari {}", type, userId);

        // Enviar en temps real via WebSocket
        try {
            NotificationResponse payload = NotificationResponse.from(notification);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    payload
            );
            log.debug("NotificationService: notificació enviada via WS a l'usuari {}", userId);
        } catch (Exception e) {
            // El WS pot no estar connectat — la notificació ja és a la BD
            log.warn("NotificationService: no s'ha pogut enviar WS a l'usuari {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Retorna les notificacions no llegides d'un usuari.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(UUID userId) {
        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    /**
     * Marca totes les notificacions d'un usuari com a llegides.
     */
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadByUserId(userId);
        log.info("NotificationService: totes les notificacions marcades com a llegides per a l'usuari {}", userId);
    }
}
