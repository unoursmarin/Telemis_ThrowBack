package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

/**
 * Translates an optimistic locking conflict (two concurrent rolls on
 * the same game) into a clear application exception for the caller.
 *
 */
public class ConcurrentRollConflictException extends RuntimeException {

    public ConcurrentRollConflictException(UUID gameSessionId, Throwable cause) {
        super("Conflit détecté sur la partie " + gameSessionId + ", réessayez le lancer", cause);
    }
}
