package com.telemisl.rcher.modules.telemisbowling.web;

import com.telemisl.rcher.modules.telemisbowling.lobby.LobbySnapshot;
import com.telemisl.rcher.modules.telemisbowling.session.GameSessionSnapshot;
import com.telemisl.rcher.modules.telemisbowling.web.dto.ApiResponse;
import com.telemisl.rcher.modules.telemisbowling.web.dto.CreateLobbyRequest;
import com.telemisl.rcher.modules.telemisbowling.web.dto.LobbyMembershipDto;
import com.telemisl.rcher.modules.telemisbowling.web.dto.SetReadyRequest;
import com.telemisl.rcher.modules.telemisbowling.web.dto.SubmitRollRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

//Covers GlobalExceptionHandler api paths
class GlobalExceptionHandlerApiTest extends AbstractApiTest {

    @Test
    @DisplayName("un lobby inconnu renvoie 404 LOBBY_NOT_FOUND")
    void unknownLobby_returnsNotFound() throws Exception {
        ApiResponse<LobbySnapshot> response = getRaw("/api/lobbies/" + UUID.randomUUID(), LobbySnapshot.class);
        assertThat(response.error().code()).isEqualTo("LOBBY_NOT_FOUND");
    }

    @Test
    @DisplayName("une partie inconnue renvoie 404 GAME_NOT_FOUND")
    void unknownGame_returnsNotFound() throws Exception {
        ApiResponse<GameSessionSnapshot> response = getRaw("/api/games/" + UUID.randomUUID(), GameSessionSnapshot.class);
        assertThat(response.error().code()).isEqualTo("GAME_NOT_FOUND");
    }

    @Test
    @DisplayName("un nom de joueur vide renvoie 400 VALIDATION_ERROR")
    void blankDisplayName_returnsValidationError() throws Exception {
        ApiResponse<LobbyMembershipDto> response = sendPostRaw(
                "/api/lobbies", new CreateLobbyRequest(""), null, 400, LobbyMembershipDto.class);
        assertThat(response.error().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("un nombre de quilles hors bornes (>15) renvoie 400 VALIDATION_ERROR avant même d'atteindre le domaine")
    void pinsOutOfBounds_returnsValidationError() throws Exception {
        LobbyMembershipDto solo = createLobby("Solo");
        setReady(solo.lobbyId(), solo.sessionToken());
        UUID gameId = start(solo.lobbyId(), solo.sessionToken()).gameSessionId();

        var response = sendPostRaw("/api/games/" + gameId + "/rolls",
                new SubmitRollRequest(16), solo.sessionToken(), 400, Object.class);
        assertThat(response.error().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("un jeton de session inconnu renvoie 401 INVALID_SESSION_TOKEN")
    void unknownSessionToken_returnsUnauthorized() throws Exception {
        LobbyMembershipDto host = createLobby("Alice");

        ApiResponse<LobbySnapshot> response = sendPostRaw("/api/lobbies/" + host.lobbyId() + "/ready",
                new SetReadyRequest(true), UUID.randomUUID(), 401, LobbySnapshot.class);
        assertThat(response.error().code()).isEqualTo("INVALID_SESSION_TOKEN");
    }

    @Test
    @DisplayName("GET /me avec un jeton étranger à la partie renvoie 401 INVALID_SESSION_TOKEN")
    void whoAmIWithForeignToken_returnsUnauthorized() throws Exception {
        LobbyMembershipDto solo = createLobby("Solo");
        setReady(solo.lobbyId(), solo.sessionToken());
        UUID gameId = start(solo.lobbyId(), solo.sessionToken()).gameSessionId();

        var response = getRaw("/api/games/" + gameId + "/me", UUID.randomUUID(), 401, Object.class);
        assertThat(response.error().code()).isEqualTo("INVALID_SESSION_TOKEN");
    }

    @Test
    @DisplayName("seul l'hôte peut démarrer la partie : 403 NOT_LOBBY_HOST sinon")
    void nonHostStarting_returnsForbidden() throws Exception {
        LobbyMembershipDto host = createLobby("Alice");
        LobbyMembershipDto other = join(host.lobbyId());
        setReady(host.lobbyId(), host.sessionToken());
        setReady(host.lobbyId(), other.sessionToken());

        var response = sendPostRaw("/api/lobbies/" + host.lobbyId() + "/start", null, other.sessionToken(), 403, Object.class);
        assertThat(response.error().code()).isEqualTo("NOT_LOBBY_HOST");
    }

    @Test
    @DisplayName("démarrer alors qu'un membre n'est pas prêt renvoie 409 LOBBY_NOT_READY")
    void startingWithUnreadyMember_returnsConflict() throws Exception {
        LobbyMembershipDto host = createLobby("Alice");
        join(host.lobbyId()); // ne se met jamais prêt
        setReady(host.lobbyId(), host.sessionToken());

        var response = sendPostRaw("/api/lobbies/" + host.lobbyId() + "/start", null, host.sessionToken(), 409, Object.class);
        assertThat(response.error().code()).isEqualTo("LOBBY_NOT_READY");
    }

    @Test
    @DisplayName("lancer après la fin de la partie renvoie 409 GAME_NOT_IN_PROGRESS")
    void rollingAfterGameCompleted_returnsConflict() throws Exception {
        LobbyMembershipDto solo = createLobby("Solo");
        setReady(solo.lobbyId(), solo.sessionToken());
        UUID gameId = start(solo.lobbyId(), solo.sessionToken()).gameSessionId();

        for (int i = 0; i < 15; i++) {
            submitRoll(gameId, solo.sessionToken(), 0);
        }

        var response = sendPostRaw("/api/games/" + gameId + "/rolls",
                new SubmitRollRequest(0), solo.sessionToken(), 409, Object.class);
        assertThat(response.error().code()).isEqualTo("GAME_NOT_IN_PROGRESS");
    }
}
