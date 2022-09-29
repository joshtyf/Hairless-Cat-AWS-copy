package com.hairlesscat.app.tournament;

public class TooManyTeamsException extends Throwable {
    public TooManyTeamsException(int numTeams, int maxNumTeams) {
        super(String.format("Too many teams in this tournament. Number of teams registered: %d, maximum allowed: %d", numTeams, maxNumTeams));
    }
}
