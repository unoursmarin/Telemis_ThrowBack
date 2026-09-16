package com.telemisl.rcher.modules.telemisbowling.domain;

// Immutable constants for the rules of the game
public final class GameRules {

    public static final int PINS_PER_FRAME = 15;
    public static final int FRAMES_PER_GAME = 5;
    public static final int BASE_ROLLS_PER_FRAME = 3;
    public static final int LAST_FRAME_MAX_ROLLS = 4;
    public static final int PERFECT_SCORE = 300;

    private GameRules() {
    }
}
