package com.hairlesscat.app.algorithm;

public class TournamentHasFinishedException extends Throwable {
    public TournamentHasFinishedException() {
        super(String.format("Bracketed tournament has finished, no other matches could be drawn"));
    }

    public TournamentHasFinishedException(String s) {
    }
}
