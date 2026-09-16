package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

// The Payload that is sent to cleint on join
public record MePayload(UUID playerId, String displayName) {
}
