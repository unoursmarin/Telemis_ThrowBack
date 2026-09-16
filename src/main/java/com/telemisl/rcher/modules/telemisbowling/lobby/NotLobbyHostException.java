package com.telemisl.rcher.modules.telemisbowling.lobby;

import java.util.UUID;

public class NotLobbyHostException extends RuntimeException {

    public NotLobbyHostException(UUID lobbyId, UUID memberId) {
        super("Le membre " + memberId + " n'est pas l'hôte du lobby " + lobbyId);
    }
}
