package com.telemisl.rcher.modules.telemisbowling.web;

import com.telemisl.rcher.modules.telemisbowling.session.GameSessionSnapshot;
import com.telemisl.rcher.modules.telemisbowling.session.RollUpdateEvent;
import com.telemisl.rcher.modules.telemisbowling.web.dto.ApiResponse;
import com.telemisl.rcher.modules.telemisbowling.web.dto.LobbyMembershipDto;
import com.telemisl.rcher.modules.telemisbowling.web.dto.SubmitRollRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

//Covers the full flow of the multiplayer game, from lobby creation to rolling the balls.
class LobbyAndGameApiTest extends AbstractApiTest {

    @Test
    @DisplayName("parcours complet : créer un lobby, rejoindre, démarrer, lancer")
    void fullMultiplayerFlow_fromLobbyCreationToRolls() throws Exception {
        LobbyMembershipDto alice = createLobby("Alice");
        LobbyMembershipDto bob = join(alice.lobbyId());

        setReady(alice.lobbyId(), alice.sessionToken());
        setReady(alice.lobbyId(), bob.sessionToken());

        UUID gameId = start(alice.lobbyId(), alice.sessionToken()).gameSessionId();

        var aliceMe = whoAmI(gameId, alice.sessionToken());
        assertThat(aliceMe.displayName()).isEqualTo("Alice");

        RollUpdateEvent aliceRoll = submitRoll(gameId, alice.sessionToken(), 5);
        assertThat(aliceRoll.player().displayName()).isEqualTo("Alice");
        assertThat(aliceRoll.player().playerId()).isEqualTo(aliceMe.playerId());
        assertThat(aliceRoll.player().frames().getFirst().rolls()).containsExactly(5);

        // A turn lasts the entire frame (3 throws): Alice has only made her
        // first throw, she keeps the turn — Bob cannot throw in her place.
        ApiResponse<RollUpdateEvent> outOfTurn = sendPostRaw(
                "/api/games/" + gameId + "/rolls", new SubmitRollRequest(3), bob.sessionToken(), 409, RollUpdateEvent.class);
        assertThat(outOfTurn.success()).isFalse();
        assertThat(outOfTurn.error().code()).isEqualTo("NOT_YOUR_TURN");

        // Alice finishes her frame (3 throws): the turn then goes to Bob.
        submitRoll(gameId, alice.sessionToken(), 4);
        submitRoll(gameId, alice.sessionToken(), 3);

        RollUpdateEvent bobRoll = submitRoll(gameId, bob.sessionToken(), 7);
        assertThat(bobRoll.player().displayName()).isEqualTo("Bob");

        GameSessionSnapshot state = getGame(gameId);
        assertThat(state.players()).hasSize(2);
    }

    @Test
    @DisplayName("un lancer invalide renvoie une erreur 400 INVALID_ROLL")
    void invalidRoll_returnsBadRequestWithErrorCode() throws Exception {
        LobbyMembershipDto solo = createLobby("Solo");
        setReady(solo.lobbyId(), solo.sessionToken());
        UUID gameId = start(solo.lobbyId(), solo.sessionToken()).gameSessionId();

        submitRoll(gameId, solo.sessionToken(), 10);
        ApiResponse<RollUpdateEvent> tooMany = sendPostRaw(
                "/api/games/" + gameId + "/rolls", new SubmitRollRequest(8), solo.sessionToken(), 400, RollUpdateEvent.class);

        assertThat(tooMany.error().code()).isEqualTo("INVALID_ROLL");
    }
}
