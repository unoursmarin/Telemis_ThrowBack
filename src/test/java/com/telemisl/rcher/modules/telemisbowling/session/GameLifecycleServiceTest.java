package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.lobby.Lobby;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyMember;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyNotFoundException;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyNotReadyToStartException;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyRepository;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyStatus;
import com.telemisl.rcher.modules.telemisbowling.lobby.NotLobbyHostException;
import com.telemisl.rcher.modules.telemisbowling.ws.GameEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameLifecycleServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private GameEventPublisher eventPublisher;

    private GameLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new GameLifecycleService(lobbyRepository, gameSessionRepository, eventPublisher);
    }

    @Test
    @DisplayName("startGame crée la partie, marque le lobby IN_PROGRESS et publie gameStarted")
    void startGame_createsSessionMarksLobbyInProgressAndPublishesGameStarted() {
        Lobby lobby = readyTwoPlayerLobby();
        LobbyMember host = lobby.getMembers().get(0);
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        GameSession session = service.startGame(lobbyId, host.getSessionToken());

        assertThat(session.getLobbyId()).isEqualTo(lobbyId);
        assertThat(session.getPlayers()).hasSize(2);
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);

        ArgumentCaptor<GameSession> sessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getId()).isEqualTo(session.getId());

        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.IN_PROGRESS);
        assertThat(lobby.getGameSessionId()).isEqualTo(session.getId());
        verify(lobbyRepository).save(lobby);

        verify(eventPublisher).publishLobbyEvent(lobbyId, "gameStarted", new GameStartedPayload(session.getId()));
    }

    @Test
    @DisplayName("startGame sur un lobby inconnu lève LobbyNotFoundException sans rien enregistrer")
    void startGame_unknownLobby_throwsWithoutSaving() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.startGame(unknown, UUID.randomUUID()))
                .isInstanceOf(LobbyNotFoundException.class);
        verifyNoInteractions(gameSessionRepository, lobbyRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("startGame par un non-hôte lève NotLobbyHostException sans rien enregistrer")
    void startGame_byNonHost_throwsWithoutSaving() {
        Lobby lobby = readyTwoPlayerLobby();
        LobbyMember bob = lobby.getMembers().get(1);
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> service.startGame(lobbyId, bob.getSessionToken()))
                .isInstanceOf(NotLobbyHostException.class);
        verifyNoInteractions(gameSessionRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("startGame avec un membre non prêt lève LobbyNotReadyToStartException sans rien enregistrer")
    void startGame_withUnreadyMember_throwsWithoutSaving() {
        Lobby lobby = openTwoPlayerLobby();
        LobbyMember host = lobby.getMembers().get(0);
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> service.startGame(lobbyId, host.getSessionToken()))
                .isInstanceOf(LobbyNotReadyToStartException.class);
        verifyNoInteractions(gameSessionRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("startGame transmet le displayName et le jeton de chaque membre à la partie")
    void startGame_propagatesMemberIdentitiesToSession() {
        Lobby lobby = readyTwoPlayerLobby();
        LobbyMember host = lobby.getMembers().get(0);
        LobbyMember bob = lobby.getMembers().get(1);
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        GameSession session = service.startGame(lobbyId, host.getSessionToken());

        List<PlayerToRegister> expected = List.of(
                new PlayerToRegister("Alice", host.getSessionToken()),
                new PlayerToRegister("Bob", bob.getSessionToken()));
        assertThat(session.getPlayers()).hasSize(2);
        assertThat(session.getPlayers().get(0).getDisplayName()).isEqualTo("Alice");
        assertThat(session.getPlayers().get(0).getSessionToken()).isEqualTo(host.getSessionToken());
        assertThat(session.getPlayers().get(1).getDisplayName()).isEqualTo("Bob");
        assertThat(session.getPlayers().get(1).getSessionToken()).isEqualTo(bob.getSessionToken());
    }

    private static Lobby readyTwoPlayerLobby() {
        Lobby lobby = openTwoPlayerLobby();
        lobby.getMembers().forEach(member -> lobby.setMemberReady(member.getSessionToken(), true));
        return lobby;
    }

    private static Lobby openTwoPlayerLobby() {
        Lobby lobby = Lobby.create();
        lobby.addMember("Alice");
        lobby.addMember("Bob");
        return lobby;
    }
}