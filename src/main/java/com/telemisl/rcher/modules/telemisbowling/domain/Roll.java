package com.telemisl.rcher.modules.telemisbowling.domain;

// Roll Of the game, how much pins have been knocked out
public record Roll(int pins) {

    public Roll {
        if (pins < 0 || pins > GameRules.PINS_PER_FRAME) {
            throw new InvalidRollException(
                    "Le nombre de quilles abattues doit être compris entre 0 et %d, reçu : %d"
                            .formatted(GameRules.PINS_PER_FRAME, pins));
        }
    }
}
