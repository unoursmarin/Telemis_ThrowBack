package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

public class NotPlayerTurnException extends RuntimeException {

    public NotPlayerTurnException(UUID gameSessionId, UUID attemptedSessionToken) {
        super("Ce n'est pas le tour de ce joueur dans la partie " + gameSessionId);
    }
}
