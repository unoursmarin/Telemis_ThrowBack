package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.domain.InvalidRollException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameSessionTest {

    private static final UUID LOBBY_ID = UUID.randomUUID();

    @Test
    @DisplayName("le premier joueur enregistré a la main en premier")
    void firstRegisteredPlayer_playsFirst() {
        GameSession session = twoPlayerSession();

        RollOutcome outcome = session.registerRoll(session.getPlayers().getFirst().getSessionToken(), 5);

        assertThat(outcome.player().getId()).isEqualTo(session.getPlayers().getFirst().getId());
    }

    @Test
    @DisplayName("un lancer soumis hors tour est rejeté")
    void rollOutOfTurn_isRejected() {
        GameSession session = twoPlayerSession();
        UUID bob = session.getPlayers().get(1).getSessionToken();

        assertThatThrownBy(() -> session.registerRoll(bob, 5)).isInstanceOf(NotPlayerTurnException.class);
    }

    @Test
    @DisplayName("le joueur garde la main pour ses 3 lancers d'une frame, puis elle passe au suivant")
    void turnStaysWithPlayer_forAllRollsOfAFrame_thenAdvances() {
        GameSession session = twoPlayerSession();
        UUID alice = session.getPlayers().get(0).getSessionToken();
        UUID bob = session.getPlayers().get(1).getSessionToken();

        // Frame ouverte (no empty raster) : Alice keeps her 3 throws.
        session.registerRoll(alice, 3);
        assertThat(session.currentPlayer().getSessionToken()).isEqualTo(alice);
        session.registerRoll(alice, 3);
        assertThat(session.currentPlayer().getSessionToken()).isEqualTo(alice);
        session.registerRoll(alice, 3);

        // 3 throws no bonuses, Bob's turn now.
        assertThat(session.currentPlayer().getSessionToken()).isEqualTo(bob);
    }

    @Test
    @DisplayName("un lancer qui vide le râtelier termine la frame plus tôt et passe la main immédiatement")
    void rollThatClearsRack_endsFrameEarly_andAdvancesTurn() {
        GameSession session = twoPlayerSession();
        UUID alice = session.getPlayers().get(0).getSessionToken();
        UUID bob = session.getPlayers().get(1).getSessionToken();

        session.registerRoll(alice, 15); // strike : empty raster

        assertThat(session.currentPlayer().getSessionToken()).isEqualTo(bob);
    }

    @Test
    @DisplayName("un lancer invalide (règles du domaine) est propagé sans modifier l'état")
    void invalidRoll_isRejectedByDomainRulesAndLeavesStateUnchanged() {
        GameSession session = onePlayerSession();
        UUID solo = session.getPlayers().getFirst().getSessionToken();
        session.registerRoll(solo, 10);

        assertThatThrownBy(() -> session.registerRoll(solo, 6)).isInstanceOf(InvalidRollException.class);

        assertThat(session.currentPlayer().getSessionToken()).isEqualTo(solo);
        assertThat(session.getPlayers().getFirst().toDomainGame().frames().getFirst().rolls()).hasSize(1);
    }

    @Test
    @DisplayName("un joueur qui a terminé ses 5 frames est sauté dans la rotation (rotation par frame)")
    void completedPlayer_isSkippedInRotation() {
        GameSession session = twoPlayerSession();
        UUID alice = session.getPlayers().getFirst().getSessionToken();
        UUID bob = session.getPlayers().getLast().getSessionToken();

        // Alice plays open frames (3 throws/frame, never empty raster, 15 throws to finish her 5 frames), Bob rolls strikes
        // (1 throw/frame, except the last with his bonus throws): they alternate a full turn (a whole frame) each as long as they
        // are both still in the game.
        for (int frame = 0; frame < 4; frame++) {
            assertThat(session.currentPlayer().getSessionToken()).isEqualTo(alice);
            session.registerRoll(alice, 0);
            session.registerRoll(alice, 0);
            session.registerRoll(alice, 0);

            assertThat(session.currentPlayer().getSessionToken()).isEqualTo(bob);
            session.registerRoll(bob, 15);
        }

        // Frame 5 (last) of Alice: still 3 throws, no bonuses (never a strike).
        assertThat(session.currentPlayer().getSessionToken()).isEqualTo(alice);
        session.registerRoll(alice, 0);
        session.registerRoll(alice, 0);
        session.registerRoll(alice, 0);

        // Alice has finished her 5 frames: the turn goes directly to Bob, never back to Alice.
        assertThat(session.currentPlayer().getSessionToken()).isEqualTo(bob);

        // Frame 5 of Bob: strike + bonus throws (LAST_FRAME_MAX_ROLLS = 4 in total).
        session.registerRoll(bob, 15);
        session.registerRoll(bob, 15);
        session.registerRoll(bob, 15);
        RollOutcome last = session.registerRoll(bob, 15);

        assertThat(last.sessionCompleted()).isTrue();
        assertThat(session.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("la partie est terminée quand tous les joueurs ont fini leurs 5 frames")
    void session_completes_whenAllPlayersFinish() {
        GameSession session = onePlayerSession();
        UUID solo = session.getPlayers().getFirst().getSessionToken();

        for (int i = 0; i < 14; i++) {
            RollOutcome outcome = session.registerRoll(solo, 0);
            assertThat(outcome.sessionCompleted()).isFalse();
        }
        RollOutcome last = session.registerRoll(solo, 0);

        assertThat(last.sessionCompleted()).isTrue();
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.COMPLETED);
        assertThatThrownBy(() -> session.registerRoll(solo, 0))
                .isInstanceOf(GameSessionNotInProgressException.class);
    }

    private static GameSession twoPlayerSession() {
        return GameSession.start(LOBBY_ID, List.of(
                new PlayerToRegister("Alice", UUID.randomUUID()),
                new PlayerToRegister("Bob", UUID.randomUUID())));
    }

    private static GameSession onePlayerSession() {
        return GameSession.start(LOBBY_ID, List.of(new PlayerToRegister("Solo", UUID.randomUUID())));
    }
}
