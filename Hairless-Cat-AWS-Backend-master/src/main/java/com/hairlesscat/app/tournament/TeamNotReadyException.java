package com.hairlesscat.app.tournament;

public class TeamNotReadyException extends Exception {
    public TeamNotReadyException(String message) {
        super(message);
    }
}
