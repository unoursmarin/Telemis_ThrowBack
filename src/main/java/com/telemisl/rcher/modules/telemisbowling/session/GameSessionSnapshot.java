package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.List;
import java.util.UUID;

// Public view of the game session state
public record GameSessionSnapshot(UUID gameId, GameSessionStatus status, UUID currentPlayerId, List<PlayerStateSnapshot> players) {
}
