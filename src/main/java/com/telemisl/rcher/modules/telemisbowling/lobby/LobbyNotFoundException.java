package com.telemisl.rcher.modules.telemisbowling.lobby;

import java.util.UUID;

public class LobbyNotFoundException extends RuntimeException {

    public LobbyNotFoundException(UUID lobbyId) {
        super("Aucun lobby trouvé avec l'id " + lobbyId);
    }
}
