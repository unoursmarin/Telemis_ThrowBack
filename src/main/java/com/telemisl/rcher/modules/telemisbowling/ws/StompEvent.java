package com.telemisl.rcher.modules.telemisbowling.ws;

import java.time.Instant;
import java.util.UUID;

/**
 * Common envelope for all STOMP messages (see docs/architecture/websocket-stomp.md).
 */
public record StompEvent<T>(String type, UUID id, T payload, Instant timestamp) {

    public static <T> StompEvent<T> of(String type, UUID id, T payload) {
        return new StompEvent<>(type, id, payload, Instant.now());
    }
}
