package com.telemisl.rcher.modules.telemisbowling.lobby;

import java.util.UUID;

public class LobbyNotReadyToStartException extends RuntimeException {

    public LobbyNotReadyToStartException(UUID lobbyId) {
        super("Tous les membres du lobby " + lobbyId + " doivent être prêts avant de démarrer la partie");
    }
}
