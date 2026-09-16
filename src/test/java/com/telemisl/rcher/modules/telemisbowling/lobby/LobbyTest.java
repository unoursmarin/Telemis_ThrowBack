package com.telemisl.rcher.modules.telemisbowling.lobby;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LobbyTest {

    @Test
    @DisplayName("le créateur du lobby en devient l'hôte")
    void firstMember_becomesHost() {
        Lobby lobby = Lobby.create();

        LobbyMember host = lobby.addMember("Alice");

        assertThat(lobby.getHostMemberId()).isEqualTo(host.getId());
        assertThat(lobby.getMembers()).containsExactly(host);
    }

    @Test
    @DisplayName("un autre joueur peut rejoindre un lobby ouvert")
    void anotherPlayer_canJoinOpenLobby() {
        Lobby lobby = Lobby.create();
        lobby.addMember("Alice");

        LobbyMember bob = lobby.addMember("Bob");

        assertThat(lobby.getMembers()).hasSize(2);
        assertThat(bob.getJoinOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("on ne peut pas rejoindre un lobby qui n'est plus ouvert")
    void joining_nonOpenLobby_throws() {
        Lobby lobby = Lobby.create();
        LobbyMember host = lobby.addMember("Alice");
        lobby.setMemberReady(host.getSessionToken(), true);
        lobby.assertCanStart(host.getSessionToken());
        lobby.markInProgress(UUID.randomUUID());

        assertThatThrownBy(() -> lobby.addMember("Bob")).isInstanceOf(LobbyNotOpenException.class);
    }

    @Test
    @DisplayName("si l'hôte quitte, l'hôte suivant par ordre d'arrivée prend le relais")
    void hostLeaving_transfersHostToNextMember() {
        Lobby lobby = Lobby.create();
        LobbyMember host = lobby.addMember("Alice");
        LobbyMember bob = lobby.addMember("Bob");

        lobby.removeMember(host.getSessionToken());

        assertThat(lobby.getHostMemberId()).isEqualTo(bob.getId());
    }

    @Test
    @DisplayName("un lobby vidé de tous ses membres est abandonné")
    void lobby_emptiedOfMembers_isAbandoned() {
        Lobby lobby = Lobby.create();
        LobbyMember host = lobby.addMember("Alice");

        lobby.removeMember(host.getSessionToken());

        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.ABANDONED);
    }

    @Test
    @DisplayName("seul l'hôte peut démarrer la partie")
    void onlyHost_canStartGame() {
        Lobby lobby = Lobby.create();
        LobbyMember host = lobby.addMember("Alice");
        LobbyMember bob = lobby.addMember("Bob");
        lobby.setMemberReady(host.getSessionToken(), true);
        lobby.setMemberReady(bob.getSessionToken(), true);

        assertThatThrownBy(() -> lobby.assertCanStart(bob.getSessionToken()))
                .isInstanceOf(NotLobbyHostException.class);
    }

    @Test
    @DisplayName("la partie ne peut démarrer que si tous les membres sont prêts")
    void starting_requiresAllMembersReady() {
        Lobby lobby = Lobby.create();
        LobbyMember host = lobby.addMember("Alice");
        lobby.addMember("Bob"); // pas prêt
        lobby.setMemberReady(host.getSessionToken(), true);

        assertThatThrownBy(() -> lobby.assertCanStart(host.getSessionToken()))
                .isInstanceOf(LobbyNotReadyToStartException.class);
    }

    @Test
    @DisplayName("un jeton de session inconnu est rejeté")
    void unknownSessionToken_isRejected() {
        Lobby lobby = Lobby.create();
        lobby.addMember("Alice");

        assertThatThrownBy(() -> lobby.setMemberReady(UUID.randomUUID(), true))
                .isInstanceOf(InvalidSessionTokenException.class);
    }
}
