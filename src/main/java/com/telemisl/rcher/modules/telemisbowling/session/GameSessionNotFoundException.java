package com.telemisl.rcher.modules.telemisbowling.session;

import java.util.UUID;

public class GameSessionNotFoundException extends RuntimeException {

    public GameSessionNotFoundException(UUID gameSessionId) {
        super("Aucune partie trouvée avec l'id " + gameSessionId);
    }
}
