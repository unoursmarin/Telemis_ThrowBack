package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.domain.Frame;
import com.telemisl.rcher.modules.telemisbowling.domain.Game;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Aggregat root for a bowling game session. This is the main entity that is persisted in the database. It contains the state of the game, the players, and the current turn. The GameSession is responsible for enforcing the rules of the game and managing the state transitions.
@Entity
@Table(name = "game_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSession implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    private UUID lobbyId;

    @Enumerated(EnumType.STRING)
    private GameSessionStatus status;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("turnOrder ASC")
    private List<PlayerGameState> players = new ArrayList<>();

    private int currentPlayerIndex;

    private Instant createdAt;

    private Instant completedAt;

    @Getter(AccessLevel.NONE)
    @Transient
    private boolean isNew = true;

    public static GameSession start(UUID lobbyId, List<PlayerToRegister> players) {
        if (players.isEmpty()) {
            throw new IllegalArgumentException("At least one Player per game");
        }
        GameSession session = new GameSession();
        session.lobbyId = lobbyId;
        session.status = GameSessionStatus.IN_PROGRESS;
        session.createdAt = Instant.now();
        session.currentPlayerIndex = 0;
        int turnOrder = 0;
        for (PlayerToRegister player : players) {
            session.players.add(new PlayerGameState(session, player.displayName(), player.sessionToken(), turnOrder++));
        }
        return session;
    }

    public List<PlayerGameState> getPlayers() {
        return List.copyOf(players);
    }

    public PlayerGameState currentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public boolean isCompleted() {
        return status == GameSessionStatus.COMPLETED;
    }

    // Find the player identified by `sessionToken`, or throw if not found. This is used to validate that a request is coming from a player in this game session.
    public PlayerGameState findPlayerBySessionToken(UUID sessionToken) {
        return players.stream()
                .filter(p -> p.getSessionToken().equals(sessionToken))
                .findFirst()
                .orElseThrow(() -> new PlayerNotInGameException(id));
    }

    // Public view of the game, reused for the REST response and WebSocket broadcast. */
    public GameSessionSnapshot toSnapshot() {
        List<PlayerStateSnapshot> playerSnapshots = players.stream().map(PlayerGameState::toSnapshot).toList();
        UUID currentPlayerId = isCompleted() ? null : currentPlayer().getId();
        return new GameSessionSnapshot(id, status, currentPlayerId, playerSnapshots);
    }

    // Register a roll for the player identified by `sessionToken`, if it's their turn, then advance the turn (or complete the session if all players are done).
    public RollOutcome registerRoll(UUID sessionToken, int pins) {
        if (status != GameSessionStatus.IN_PROGRESS) {
            throw new GameSessionNotInProgressException(id);
        }
        PlayerGameState current = currentPlayer();
        if (!current.getSessionToken().equals(sessionToken)) {
            throw new NotPlayerTurnException(id, sessionToken);
        }

        //Validate the roll using domain rules before any write: if `lancer` throws an exception, no state is modified. The frame number is captured BEFORE the roll (see Game#currentFrame): once this roll is applied, the "current frame" may have already advanced to the next one.
        Game gameBeforeRoll = current.toDomainGame();
        int rolledFrameNumber = gameBeforeRoll.currentFrame().number();
        gameBeforeRoll.lancer(pins);
        current.addRoll(pins);

        advanceTurnOrCompleteSession(rolledFrameNumber);

        UUID nextPlayerId = status == GameSessionStatus.IN_PROGRESS ? currentPlayer().getId() : null;
        return new RollOutcome(current, current.toDomainGame(), isCompleted(), nextPlayerId);
    }

    // The Parameter rolledFrameNumber is the frame number that was rolled before the roll was applied. Once the roll is applied, the current frame may have advanced to the next one, so we need to use this parameter to determine if the current player has finished their turn.
    private void advanceTurnOrCompleteSession(int rolledFrameNumber) {
        if (players.stream().allMatch(p -> p.toDomainGame().isComplete())) {
            status = GameSessionStatus.COMPLETED;
            completedAt = Instant.now();
            return;
        }
        Frame rolledFrame = currentPlayer().toDomainGame().frames().get(rolledFrameNumber - 1);
        if (!rolledFrame.isComplete()) {
            // The current player has not finished this frame (remaining rolls): they keep the turn.
            return;
        }
        int next = currentPlayerIndex;
        do {
            next = (next + 1) % players.size();
        } while (players.get(next).toDomainGame().isComplete());
        currentPlayerIndex = next;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        isNew = false;
    }
}
