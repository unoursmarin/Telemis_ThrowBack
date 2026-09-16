package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.ws.GameEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final GameEventPublisher eventPublisher;

    public GameSessionService(GameSessionRepository gameSessionRepository, GameEventPublisher eventPublisher) {
        this.gameSessionRepository = gameSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RollUpdateEvent submitRoll(UUID gameSessionId, UUID sessionToken, int pins) {
        GameSession session = findSession(gameSessionId);
        try {
            RollOutcome outcome = session.registerRoll(sessionToken, pins);
            gameSessionRepository.saveAndFlush(session);

            RollUpdateEvent event = new RollUpdateEvent(gameSessionId, outcome.player().toSnapshot(),
                    outcome.nextPlayerGameStateId(), outcome.sessionCompleted());
            eventPublisher.publishGameEvent(gameSessionId, "rollRegistered", event);
            return event;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConcurrentRollConflictException(gameSessionId, e);
        }
    }

    @Transactional(readOnly = true)
    public GameSessionSnapshot get(UUID gameSessionId) {
        return findSession(gameSessionId).toSnapshot();
    }

    @Transactional(readOnly = true)
    public MePayload whoAmI(UUID gameSessionId, UUID sessionToken) {
        PlayerGameState player = findSession(gameSessionId).findPlayerBySessionToken(sessionToken);
        return new MePayload(player.getId(), player.getDisplayName());
    }

    private GameSession findSession(UUID gameSessionId) {
        return gameSessionRepository.findById(gameSessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(gameSessionId));
    }
}
