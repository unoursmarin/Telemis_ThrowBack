package com.telemisl.rcher.modules.telemisbowling.lobby;

import java.util.UUID;

public class LobbyNotOpenException extends RuntimeException {

    public LobbyNotOpenException(UUID lobbyId) {
        super("Le lobby " + lobbyId + " n'est plus ouvert");
    }
}
