package com.travelai.config;

import com.travelai.domain.auth.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig — STOMP sobre SockJS per a notificacions en temps real.
 *
 * El client subscriu:
 *   /user/queue/notifications  → notificacions personals via SimpMessagingTemplate.convertAndSendToUser()
 *
 * Endpoint de connexió:
 *   /ws  (SockJS fallback habilitat)
 *
 * JWT validation: el ChannelInterceptor valida el token JWT en cada CONNECT frame.
 * El token pot arribar com a header "Authorization: Bearer <token>" o "token: <token>".
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker en memòria per a cues /queue i temes /topic
        registry.enableSimpleBroker("/queue", "/topic");
        // Prefix per als missatges destinats als @MessageMapping dels controllers
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix per a missatges dirigits a un usuari concret (/user/{userId}/queue/notifications)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Registra el JWT ChannelInterceptor que valida el token en els frames CONNECT STOMP.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor());
    }

    // ── JWT ChannelInterceptor ───────────────────────────────────────────────

    /**
     * Intercepta frames STOMP CONNECT i valida el JWT.
     * El token es pot proporcionar via:
     *   - Header HTTP "Authorization: Bearer <token>"
     *   - Header STOMP "token: <token>"
     *
     * Si el token és invàlid o absent, llança MessageDeliveryException tancant la connexió.
     */
    private class JwtChannelInterceptor implements ChannelInterceptor {

        private static final String BEARER_PREFIX = "Bearer ";

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                // Only intercept CONNECT frames; allow all other frames through
                return message;
            }

            String token = extractToken(accessor);

            if (token == null || token.isBlank()) {
                log.warn("WebSocket CONNECT sense token JWT — connexió rebutjada");
                throw new MessageDeliveryException("Token JWT requerit per connectar al WebSocket");
            }

            if (!jwtService.validateToken(token) || !jwtService.isAccessToken(token)) {
                log.warn("WebSocket CONNECT amb token JWT invàlid o expirat — connexió rebutjada");
                throw new MessageDeliveryException("Token JWT invàlid o expirat");
            }

            log.debug("WebSocket CONNECT autenticat per userId: {}", jwtService.extractUserId(token));
            return message;
        }

        /**
         * Extreu el token JWT dels headers STOMP o del header Authorization HTTP.
         */
        private String extractToken(StompHeaderAccessor accessor) {
            // 1. Intent via header STOMP "token"
            String tokenHeader = accessor.getFirstNativeHeader("token");
            if (tokenHeader != null && !tokenHeader.isBlank()) {
                return tokenHeader.trim();
            }

            // 2. Intent via header "Authorization: Bearer <token>"
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                return authHeader.substring(BEARER_PREFIX.length()).trim();
            }

            return null;
        }
    }
}
