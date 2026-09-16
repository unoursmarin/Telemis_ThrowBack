package com.telemisl.rcher.modules.telemisbowling.domain;

//Case end game and we still throw
public class GameAlreadyCompleteException extends RuntimeException {

    public GameAlreadyCompleteException(String message) {
        super(message);
    }
}
