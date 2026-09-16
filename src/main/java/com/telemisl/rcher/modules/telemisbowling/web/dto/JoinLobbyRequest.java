package com.telemisl.rcher.modules.telemisbowling.web.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinLobbyRequest(@NotBlank String displayName) {
}
