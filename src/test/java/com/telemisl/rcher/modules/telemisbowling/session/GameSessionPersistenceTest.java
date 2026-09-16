package com.telemisl.rcher.modules.telemisbowling.session;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * checks mapping JPA
 */
@DataJpaTest
class GameSessionPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Test
    void savedGameSession_reloadsWithPlayersAndRollsInOrder() {
        UUID lobbyId = UUID.randomUUID();
        GameSession session = GameSession.start(lobbyId, List.of(
                new PlayerToRegister("Alice", UUID.randomUUID()),
                new PlayerToRegister("Bob", UUID.randomUUID())));
        session.registerRoll(session.getPlayers().getFirst().getSessionToken(), 8);

        UUID id = entityManager.persistAndFlush(session).getId();
        entityManager.clear();

        assert id != null;
        GameSession reloaded = gameSessionRepository.findById(id).orElseThrow();

        assertThat(reloaded.getLobbyId()).isEqualTo(lobbyId);
        assertThat(reloaded.getPlayers()).hasSize(2);
        assertThat(reloaded.getPlayers().getFirst().toDomainGame().frames().getFirst().rolls()).hasSize(1);
        // 8 quilles sur 15 : Alice's frame hasn't been completed yet, so currentPlayerIndex is still 0 (Alice) and not 1 (Bob)
        assertThat(reloaded.currentPlayer().getDisplayName()).isEqualTo("Alice");
    }

    @Test
    void concurrentRollOnStaleVersion_throwsOptimisticLockException() {
        // Two players register and play a roll. Then, we load the same game session twice (stale and fresh). The fresh one plays a roll and saves it. The stale one tries to play a roll and save it, which should throw an optimistic lock exception.
        GameSession session = GameSession.start(UUID.randomUUID(), List.of(
                new PlayerToRegister("Alice", UUID.randomUUID()),
                new PlayerToRegister("Bob", UUID.randomUUID())));
        UUID aliceToken = session.getPlayers().getFirst().getSessionToken();
        UUID id = entityManager.persistAndFlush(session).getId();
        entityManager.clear();

        assert id != null;
        GameSession stale = gameSessionRepository.findById(id).orElseThrow();
        // Lazy collection loading
        // detach : no more hibernate session
        stale.getPlayers().forEach(PlayerGameState::toDomainGame);
        entityManager.detach(stale);

        GameSession fresh = gameSessionRepository.findById(id).orElseThrow();
        fresh.registerRoll(aliceToken, 15);
        gameSessionRepository.saveAndFlush(fresh);

        stale.registerRoll(aliceToken, 4);

        assertThatThrownBy(() -> gameSessionRepository.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
