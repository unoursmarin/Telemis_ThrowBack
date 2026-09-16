package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.ws.GameEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//Determining the behavior of GameSessionService when handling optimistic locking failures and successful roll submissions.
@ExtendWith(MockitoExtension.class)
class GameSessionServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private GameEventPublisher eventPublisher;

    private GameSessionService service;

    @BeforeEach
    void setUp() {
        service = new GameSessionService(gameSessionRepository, eventPublisher);
    }

    @Test
    @DisplayName("un conflit de version optimiste est traduit en ConcurrentRollConflictException, sans publier d'événement")
    void optimisticLockFailure_isTranslatedAndNoEventPublished() {
        GameSession session = soloSession();
        UUID gameId = session.getId();
        UUID token = session.getPlayers().getFirst().getSessionToken();
        assert gameId != null;
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameSessionRepository.saveAndFlush(session))
                .thenThrow(new ObjectOptimisticLockingFailureException(GameSession.class, gameId));

        assertThatThrownBy(() -> service.submitRoll(gameId, token, 5))
                .isInstanceOf(ConcurrentRollConflictException.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("un lancer réussi publie l'événement rollRegistered sur /topic/games/{id}")
    void successfulRoll_publishesRollRegisteredEvent() {
        GameSession session = soloSession();
        UUID gameId = session.getId();
        UUID token = session.getPlayers().getFirst().getSessionToken();
        assert gameId != null;
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));
        when(gameSessionRepository.saveAndFlush(session)).thenReturn(session);

        RollUpdateEvent event = service.submitRoll(gameId, token, 5);

        assertThat(event.player().displayName()).isEqualTo("Solo");
        assertThat(event.sessionCompleted()).isFalse();
        verify(eventPublisher).publishGameEvent(eq(gameId), eq("rollRegistered"), any(RollUpdateEvent.class));
    }

    @Test
    @DisplayName("get renvoie l'état public de la partie")
    void get_returnsSnapshot() {
        GameSession session = soloSession();
        UUID gameId = session.getId();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        GameSessionSnapshot snapshot = service.get(gameId);

        assertThat(snapshot.gameId()).isEqualTo(gameId);
        assertThat(snapshot.status()).isEqualTo(GameSessionStatus.IN_PROGRESS);
        assertThat(snapshot.currentPlayerId()).isEqualTo(session.getPlayers().get(0).getId());
        assertThat(snapshot.players()).hasSize(1);
        assertThat(snapshot.players().get(0).displayName()).isEqualTo("Solo");
    }

    @Test
    @DisplayName("get sur une partie inconnue lève GameSessionNotFoundException")
    void get_unknownSession_throws() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(GameSessionNotFoundException.class);
    }

    @Test
    @DisplayName("whoAmI renvoie le joueur propriétaire du jeton")
    void whoAmI_knownToken_returnsPlayerIdentity() {
        GameSession session = soloSession();
        UUID gameId = session.getId();
        UUID token = session.getPlayers().get(0).getSessionToken();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        MePayload payload = service.whoAmI(gameId, token);

        assertThat(payload.playerId()).isEqualTo(session.getPlayers().get(0).getId());
        assertThat(payload.displayName()).isEqualTo("Solo");
    }

    @Test
    @DisplayName("whoAmI avec un jeton étranger à la partie lève PlayerNotInGameException")
    void whoAmI_foreignToken_throws() {
        GameSession session = soloSession();
        UUID gameId = session.getId();
        when(gameSessionRepository.findById(gameId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.whoAmI(gameId, UUID.randomUUID()))
                .isInstanceOf(PlayerNotInGameException.class);
    }

    @Test
    @DisplayName("submitRoll sur une partie inconnue lève GameSessionNotFoundException")
    void submitRoll_unknownSession_throws() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.submitRoll(unknown, UUID.randomUUID(), 5))
                .isInstanceOf(GameSessionNotFoundException.class);
    }

    private static GameSession soloSession() {
        return GameSession.start(UUID.randomUUID(), List.of(new PlayerToRegister("Solo", UUID.randomUUID())));
    }
}
