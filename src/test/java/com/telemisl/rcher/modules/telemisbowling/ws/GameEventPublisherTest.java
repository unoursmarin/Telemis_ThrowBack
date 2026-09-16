package com.telemisl.rcher.modules.telemisbowling.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private GameEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new GameEventPublisher(messagingTemplate);
    }

    @Test
    @DisplayName("publishLobbyEvent diffuse un StompEvent typé sur /topic/lobbies/{id}")
    void publishLobbyEvent_sendsTypedStompEventToLobbyTopic() {
        UUID lobbyId = UUID.randomUUID();
        LobbyPayload payload = new LobbyPayload("Alice");

        publisher.publishLobbyEvent(lobbyId, "playerJoined", payload);

        ArgumentCaptor<StompEvent<?>> captor = ArgumentCaptor.forClass(StompEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/lobbies/" + lobbyId), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("playerJoined");
        assertThat(captor.getValue().id()).isEqualTo(lobbyId);
        assertThat(captor.getValue().payload()).isEqualTo(payload);
        assertThat(captor.getValue().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("publishGameEvent diffuse un StompEvent typé sur /topic/games/{id}")
    void publishGameEvent_sendsTypedStompEventToGameTopic() {
        UUID gameId = UUID.randomUUID();
        GamePayload payload = new GamePayload(42);

        publisher.publishGameEvent(gameId, "rollRegistered", payload);

        ArgumentCaptor<StompEvent<?>> captor = ArgumentCaptor.forClass(StompEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/games/" + gameId), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("rollRegistered");
        assertThat(captor.getValue().id()).isEqualTo(gameId);
        assertThat(captor.getValue().payload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("une exception de diffusion est avalée et ne remonte pas à l'appelant")
    void messagingFailure_isSwallowed() {
        doThrow(new MessagingException("broker down"))
                .when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

        assertThatCode(() -> publisher.publishGameEvent(UUID.randomUUID(), "rollRegistered", new GamePayload(1)))
                .doesNotThrowAnyException();
    }

    private record LobbyPayload(String displayName) {
    }

    private record GamePayload(int pins) {
    }
}