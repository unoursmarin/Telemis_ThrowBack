package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

//Result of a roll sent to client after a roll is made
public record RollUpdateEvent(UUID gameId, PlayerStateSnapshot player, UUID nextPlayerId, boolean sessionCompleted) {
}
