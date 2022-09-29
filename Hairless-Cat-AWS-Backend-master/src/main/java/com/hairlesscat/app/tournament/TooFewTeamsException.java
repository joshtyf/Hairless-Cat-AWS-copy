package com.hairlesscat.app.tournament;

public class TooFewTeamsException extends Exception {
    public TooFewTeamsException(int numOfTeams, int minNumOfTeams) {
        super(String.format("The required number of teams in this tournament has not been met. Number of teams currently: %d, number of required teams: %d.", numOfTeams, minNumOfTeams));
    }
}
