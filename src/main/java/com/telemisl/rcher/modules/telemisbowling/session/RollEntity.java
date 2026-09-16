package com.telemisl.rcher.modules.telemisbowling.session;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// Persists the history of a roll
@Entity
@Table(name = "rolls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @ManyToOne(optional = false)
    @JoinColumn(name = "player_game_state_id", nullable = false)
    private PlayerGameState playerGameState;

    private int rollOrder;

    private int pins;

    private Instant createdAt;

    RollEntity(PlayerGameState playerGameState, int rollOrder, int pins) {
        this.playerGameState = playerGameState;
        this.rollOrder = rollOrder;
        this.pins = pins;
        this.createdAt = Instant.now();
    }
}
