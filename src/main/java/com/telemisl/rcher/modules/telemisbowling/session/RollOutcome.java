package com.telemisl.rcher.modules.telemisbowling.session;

import com.telemisl.rcher.modules.telemisbowling.domain.Game;

import java.util.UUID;

// Results of a roll, including the updated player state
public record RollOutcome(PlayerGameState player, Game game, boolean sessionCompleted, UUID nextPlayerGameStateId) {
}
