package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.domain.Frame;
import com.telemisl.rcher.modules.telemisbowling.domain.Game;
import com.telemisl.rcher.modules.telemisbowling.domain.Roll;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


// Game state within a session for a player their score and session is built here  and on demand via toDomainGame
@Entity
@Table(name = "player_game_states")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerGameState implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Getter(AccessLevel.NONE)
    @ManyToOne(optional = false)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    private String displayName;

    private UUID sessionToken;

    private int turnOrder;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "playerGameState", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rollOrder ASC")
    private List<RollEntity> rolls = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Transient
    private boolean isNew = true;

    PlayerGameState(GameSession gameSession, String displayName, UUID sessionToken, int turnOrder) {
        this.gameSession = gameSession;
        this.displayName = displayName;
        this.sessionToken = sessionToken;
        this.turnOrder = turnOrder;
    }

    void addRoll(int pins) {
        rolls.add(new RollEntity(this, rolls.size(), pins));
    }

    /** Reconstructs the game state for this player by replaying the persisted history of their rolls. */
    public Game toDomainGame() {
        Game game = new Game();
        for (RollEntity roll : rolls) {
            game.lancer(roll.getPins());
        }
        return game;
    }

    /** Public view of this player's state, reused for the REST response and WebSocket broadcast. */
    public PlayerStateSnapshot toSnapshot() {
        Game game = toDomainGame();
        List<Frame> frames = game.frames();
        List<Integer> scores = game.frameScores();
        List<FrameSnapshot> frameSnapshots = new ArrayList<>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            List<Integer> pins = frame.rolls().stream().map(Roll::pins).toList();
            frameSnapshots.add(new FrameSnapshot(frame.number(), pins, frame.status(), scores.get(i)));
        }
        return new PlayerStateSnapshot(id, displayName, frameSnapshots, game.score(), game.isComplete());
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
