package com.telemisl.rcher.modules.telemisbowling.lobby;

import com.telemisl.rcher.modules.telemisbowling.ws.GameEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private GameEventPublisher eventPublisher;

    private LobbyService service;

    @BeforeEach
    void setUp() {
        service = new LobbyService(lobbyRepository, eventPublisher);
    }

    @Test
    @DisplayName("createLobby crée un lobby ouvert, enregistre l'hôte et persiste")
    void createLobby_createsOpenLobbyWithHostAndSaves() {
        LobbyMember host = service.createLobby("Alice");

        assertThat(host.getDisplayName()).isEqualTo("Alice");
        assertThat(host.getJoinOrder()).isZero();
        assertThat(host.isReady()).isFalse();
        verify(lobbyRepository).save(any(Lobby.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("join ajoute le membre, persiste et publie playerJoined avec l'état du lobby")
    void join_addsMemberSavesAndPublishesPlayerJoined() {
        Lobby lobby = openLobbyWithHost();
        LobbyMember alice = lobby.getMembers().get(0);
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        LobbyMember bob = service.join(lobbyId, "Bob");

        assertThat(bob.getDisplayName()).isEqualTo("Bob");
        assertThat(bob.getJoinOrder()).isEqualTo(1);
        verify(lobbyRepository).save(lobby);
        ArgumentCaptor<LobbySnapshot> captor = ArgumentCaptor.forClass(LobbySnapshot.class);
        verify(eventPublisher).publishLobbyEvent(lobbyId, "playerJoined", captor.capture());
        assertThat(captor.getValue().members()).hasSize(2);
        assertThat(captor.getValue().members().get(1).displayName()).isEqualTo("Bob");
        assertThat(captor.getValue().hostMemberId()).isEqualTo(alice.getId());
    }

    @Test
    @DisplayName("leave retire le membre, persiste et publie playerLeft")
    void leave_removesMemberSavesAndPublishesPlayerLeft() {
        Lobby lobby = openLobbyWithHost();
        LobbyMember alice = lobby.getMembers().get(0);
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        service.leave(lobbyId, alice.getSessionToken());

        assertThat(lobby.getMembers()).isEmpty();
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.ABANDONED);
        verify(lobbyRepository).save(lobby);
        ArgumentCaptor<LobbySnapshot> captor = ArgumentCaptor.forClass(LobbySnapshot.class);
        verify(eventPublisher).publishLobbyEvent(lobbyId, "playerLeft", captor.capture());
        assertThat(captor.getValue().members()).isEmpty();
    }

    @Test
    @DisplayName("setReady marque le membre comme prêt, persiste et publie playerReadyChanged")
    void setReady_marksMemberReadySavesAndPublishesPlayerReadyChanged() {
        Lobby lobby = openLobbyWithHost();
        LobbyMember alice = lobby.getMembers().get(0);
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        LobbySnapshot snapshot = service.setReady(lobbyId, alice.getSessionToken(), true);

        assertThat(alice.isReady()).isTrue();
        assertThat(snapshot.members().get(0).ready()).isTrue();
        verify(lobbyRepository).save(lobby);
        ArgumentCaptor<LobbySnapshot> captor = ArgumentCaptor.forClass(LobbySnapshot.class);
        verify(eventPublisher).publishLobbyEvent(lobbyId, "playerReadyChanged", captor.capture());
        assertThat(captor.getValue().members().get(0).ready()).isTrue();
    }

    @Test
    @DisplayName("get renvoie l'état public du lobby sans publier d'événement")
    void get_returnsSnapshotWithoutPublishing() {
        Lobby lobby = openLobbyWithHost();
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        LobbySnapshot snapshot = service.get(lobbyId);

        assertThat(snapshot.lobbyId()).isEqualTo(lobbyId);
        assertThat(snapshot.status()).isEqualTo(LobbyStatus.OPEN);
        assertThat(snapshot.hostMemberId()).isEqualTo(lobby.getHostMemberId());
        assertThat(snapshot.members()).hasSize(1);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("chaque opération sur un lobby inconnu lève LobbyNotFoundException")
    void unknownLobby_throwsForEveryOperation() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(LobbyNotFoundException.class);
        assertThatThrownBy(() -> service.join(unknown, "Bob")).isInstanceOf(LobbyNotFoundException.class);
        assertThatThrownBy(() -> service.leave(unknown, UUID.randomUUID())).isInstanceOf(LobbyNotFoundException.class);
        assertThatThrownBy(() -> service.setReady(unknown, UUID.randomUUID(), true))
                .isInstanceOf(LobbyNotFoundException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("setReady sur un jeton inconnu lève InvalidSessionTokenException sans persister")
    void setReady_withUnknownToken_throwsWithoutPublishing() {
        Lobby lobby = openLobbyWithHost();
        UUID lobbyId = lobby.getId();
        when(lobbyRepository.findById(lobbyId)).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> service.setReady(lobbyId, UUID.randomUUID(), true))
                .isInstanceOf(InvalidSessionTokenException.class);
        verifyNoInteractions(eventPublisher);
    }

    private static Lobby openLobbyWithHost() {
        Lobby lobby = Lobby.create();
        lobby.addMember("Alice");
        return lobby;
    }
}