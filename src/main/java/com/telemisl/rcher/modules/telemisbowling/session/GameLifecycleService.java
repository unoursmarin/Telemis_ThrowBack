package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.lobby.Lobby;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyNotFoundException;
import com.telemisl.rcher.modules.telemisbowling.lobby.LobbyRepository;
import com.telemisl.rcher.modules.telemisbowling.ws.GameEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// GameLifecycleService is responsible for managing the lifecycle of a game session, including starting a new game session from a lobby. It interacts with the LobbyRepository to retrieve and update lobby information, the GameSessionRepository to persist game session data, and the GameEventPublisher to notify clients about game events.
@Service
public class GameLifecycleService {

    private final LobbyRepository lobbyRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GameEventPublisher eventPublisher;

    public GameLifecycleService(LobbyRepository lobbyRepository, GameSessionRepository gameSessionRepository,
                                 GameEventPublisher eventPublisher) {
        this.lobbyRepository = lobbyRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public GameSession startGame(UUID lobbyId, UUID requestingSessionToken) {
        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new LobbyNotFoundException(lobbyId));
        lobby.assertCanStart(requestingSessionToken);

        List<PlayerToRegister> players = lobby.getMembers().stream()
                .map(member -> new PlayerToRegister(member.getDisplayName(), member.getSessionToken()))
                .toList();

        GameSession session = GameSession.start(lobbyId, players);
        gameSessionRepository.save(session);

        lobby.markInProgress(session.getId());
        lobbyRepository.save(lobby);

        eventPublisher.publishLobbyEvent(lobbyId, "gameStarted", new GameStartedPayload(session.getId()));

        return session;
    }
}
