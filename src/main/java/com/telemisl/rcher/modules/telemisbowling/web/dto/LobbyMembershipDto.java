package com.telemisl.rcher.modules.telemisbowling.web.dto;

import java.util.UUID;

// Response returned only to the member who just created or joined the lobby: carries their session token, never expose to other members.
public record LobbyMembershipDto(UUID lobbyId, UUID memberId, String displayName, UUID sessionToken) {
}
