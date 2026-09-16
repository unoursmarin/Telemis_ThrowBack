package com.telemisl.rcher.modules.telemisbowling.domain;

// Thrown on invalid roll (too much points etc)
public class InvalidRollException extends RuntimeException {

    public InvalidRollException(String message) {
        super(message);
    }
}
