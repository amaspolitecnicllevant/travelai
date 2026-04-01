package com.travelai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
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
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
}
