package com.telemisl.rcher.modules.telemisbowling.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Displays Event webSocket messages to the clients subscribed to the corresponding topics.
@Component
public class GameEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(GameEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public GameEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishLobbyEvent(UUID lobbyId, String type, Object payload) {
        send("/topic/lobbies/" + lobbyId, type, lobbyId, payload);
    }

    public void publishGameEvent(UUID gameId, String type, Object payload) {
        send("/topic/games/" + gameId, type, gameId, payload);
    }

    private void send(String destination, String type, UUID id, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, StompEvent.of(type, id, payload));
        } catch (MessagingException e) {
            log.warn("Échec de diffusion de l'événement '{}' sur {}", type, destination, e);
        }
    }
}
