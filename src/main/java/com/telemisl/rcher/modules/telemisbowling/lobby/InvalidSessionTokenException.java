package com.telemisl.rcher.modules.telemisbowling.lobby;

public class InvalidSessionTokenException extends RuntimeException {

    public InvalidSessionTokenException(String message) {
        super(message);
    }
}
