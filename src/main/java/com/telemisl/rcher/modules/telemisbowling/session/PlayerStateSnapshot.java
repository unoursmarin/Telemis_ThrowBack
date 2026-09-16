package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.List;
import java.util.UUID;

public record PlayerStateSnapshot(UUID playerId, String displayName, List<FrameSnapshot> frames, int totalScore, boolean complete) {
}
