package com.telemisl.rcher.modules.telemisbowling.web;

import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyMember;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyService;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbySnapshot;
import com.telemisl.rcher.modules.telemisbowling.session.GameLifecycleService;
import com.telemisl.rcher.modules.telemisbowling.session.GameSession;
import com.telemisl.rcher.modules.telemisbowling.session.GameStartedPayload;
import com.telemisl.rcher.modules.telemisbowling.web.dto.ApiResponse;
import com.telemisl.rcher.modules.telemisbowling.web.dto.CreateLobbyRequest;
import com.telemisl.rcher.modules.telemisbowling.web.dto.JoinLobbyRequest;
import com.telemisl.rcher.modules.telemisbowling.web.dto.LobbyMembershipDto;
import com.telemisl.rcher.modules.telemisbowling.web.dto.SetReadyRequest;
import com.telemisl.rcher.modules.telemisbowling.web.mapper.LobbyMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Handles HTTP requests related to lobbies.
@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {

    private final LobbyService lobbyService;
    private final GameLifecycleService gameLifecycleService;
    private final LobbyMapper lobbyMapper;

    public LobbyController(LobbyService lobbyService, GameLifecycleService gameLifecycleService, LobbyMapper lobbyMapper) {
        this.lobbyService = lobbyService;
        this.gameLifecycleService = gameLifecycleService;
        this.lobbyMapper = lobbyMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LobbyMembershipDto>> createLobby(@Valid @RequestBody CreateLobbyRequest request) {
        LobbyMember host = lobbyService.createLobby(request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(lobbyMapper.toMembershipDto(host)));
    }

    @GetMapping("/{lobbyId}")
    public ApiResponse<LobbySnapshot> getLobby(@PathVariable UUID lobbyId) {
        return ApiResponse.ok(lobbyService.get(lobbyId));
    }

    @PostMapping("/{lobbyId}/join")
    public ResponseEntity<ApiResponse<LobbyMembershipDto>> join(@PathVariable UUID lobbyId,
                                                                 @Valid @RequestBody JoinLobbyRequest request) {
        LobbyMember member = lobbyService.join(lobbyId, request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(lobbyMapper.toMembershipDto(member)));
    }

    @PostMapping("/{lobbyId}/leave")
    public ApiResponse<Void> leave(@PathVariable UUID lobbyId, @RequestHeader("X-Session-Token") UUID sessionToken) {
        lobbyService.leave(lobbyId, sessionToken);
        return ApiResponse.<Void>ok(null);
    }

    @PostMapping("/{lobbyId}/ready")
    public ApiResponse<LobbySnapshot> setReady(@PathVariable UUID lobbyId,
                                                @RequestHeader("X-Session-Token") UUID sessionToken,
                                                @Valid @RequestBody SetReadyRequest request) {
        return ApiResponse.ok(lobbyService.setReady(lobbyId, sessionToken, request.ready()));
    }

    @PostMapping("/{lobbyId}/start")
    public ApiResponse<GameStartedPayload> start(@PathVariable UUID lobbyId,
                                                  @RequestHeader("X-Session-Token") UUID sessionToken) {
        GameSession session = gameLifecycleService.startGame(lobbyId, sessionToken);
        return ApiResponse.ok(new GameStartedPayload(session.getId()));
    }
}
