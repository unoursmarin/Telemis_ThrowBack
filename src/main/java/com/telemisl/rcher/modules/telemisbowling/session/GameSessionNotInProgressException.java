package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

public class GameSessionNotInProgressException extends RuntimeException {

    public GameSessionNotInProgressException(UUID gameSessionId) {
        super("La partie " + gameSessionId + " n'est plus en cours");
    }
}
