package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

public class PlayerNotInGameException extends RuntimeException {

    public PlayerNotInGameException(UUID gameSessionId) {
        super("Aucun joueur de la partie " + gameSessionId + " ne correspond à ce jeton de session");
    }
}
