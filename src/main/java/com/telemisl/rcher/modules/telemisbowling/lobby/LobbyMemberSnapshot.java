package com.telemisl.rcher.modules.telemisbowling.lobby;

import java.util.UUID;

// Public member view
public record LobbyMemberSnapshot(UUID memberId, String displayName, boolean ready) {
}
