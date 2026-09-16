package com.telemisl.rcher.modules.telemisbowling.web;

import com.telemisl.rcher.modules.telemisbowling.session.GameSessionService;
import com.telemisl.rcher.modules.telemisbowling.session.GameSessionSnapshot;
import com.telemisl.rcher.modules.telemisbowling.session.MePayload;
import com.telemisl.rcher.modules.telemisbowling.session.RollUpdateEvent;
import com.telemisl.rcher.modules.telemisbowling.web.dto.ApiResponse;
import com.telemisl.rcher.modules.telemisbowling.web.dto.SubmitRollRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

//The session token decides who is throwing, not the URL. The URL is just for the game ID.
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameSessionService gameSessionService;

    public GameController(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    @GetMapping("/{gameId}")
    public ApiResponse<GameSessionSnapshot> getGame(@PathVariable UUID gameId) {
        return ApiResponse.ok(gameSessionService.get(gameId));
    }

    @GetMapping("/{gameId}/me")
    public ApiResponse<MePayload> whoAmI(@PathVariable UUID gameId,
                                          @RequestHeader("X-Session-Token") UUID sessionToken) {
        return ApiResponse.ok(gameSessionService.whoAmI(gameId, sessionToken));
    }

    @PostMapping("/{gameId}/rolls")
    public ApiResponse<RollUpdateEvent> submitRoll(@PathVariable UUID gameId,
                                                    @RequestHeader("X-Session-Token") UUID sessionToken,
                                                    @Valid @RequestBody SubmitRollRequest request) {
        return ApiResponse.ok(gameSessionService.submitRoll(gameId, sessionToken, request.pins()));
    }
}
