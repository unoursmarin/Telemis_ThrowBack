package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

// Stomp Payload  that announces the game has started
public record GameStartedPayload(UUID gameSessionId) {
}
