package com.telemisl.rcher.modules.telemisbowling.lobby;

import java.util.List;
import java.util.UUID;

/**
 * Public view of a lobby used for query and events
 */
public record LobbySnapshot(
        UUID lobbyId,
        LobbyStatus status,
        UUID hostMemberId,
        UUID gameSessionId,
        List<LobbyMemberSnapshot> members) {
}
