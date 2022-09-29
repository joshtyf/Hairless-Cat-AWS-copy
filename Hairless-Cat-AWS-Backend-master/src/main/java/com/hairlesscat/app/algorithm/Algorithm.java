package com.hairlesscat.app.algorithm;

import com.hairlesscat.app.match.Match;
import com.hairlesscat.app.result.Result;
import com.hairlesscat.app.round.Round;
import com.hairlesscat.app.team.Team;
import com.hairlesscat.app.tournament.Tournament;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;

import java.util.ArrayList;
import java.util.List;

public class Algorithm {

    /**
     * Generates a list of matches for a round robin style tournament.
     * A round robin style implies that each team has to play every other team at least once.
     * @param tournamentTimeslots timeslots that can be played for the tournament.
     * @param teams list of teams in the tournament.
     * @param isDoubleRoundRobin a boolean indicating if the matches need to be scheduled twice.
     * @return a scheduled list of matches.
     * @throws ImperfectMatchingException if the scheduling failed because one or more matches are not assigned a timeslot.
     * @throws MoreMatchesThanAvailableTimeslotsException if the number of timeslots are too few compared to the number of matches required to be played.
     */
    public static List<Match> genRRMatches(List<TournamentTimeslot> tournamentTimeslots, List<Team> teams, boolean isDoubleRoundRobin, Tournament tournament, Round round) throws ImperfectMatchingException, MoreMatchesThanAvailableTimeslotsException {
        int numTeams = teams.size();
        List<Match> unscheduledMatches = new ArrayList<>();

        for (int i = 0; i < numTeams; i++) {
            for (int j = i + 1; j < numTeams; j++) {
                Team team1 = teams.get(i);
                Team team2 = teams.get(j);
                Match match = Match.incompleteBuildWithTeamsOnly(List.of(team1, team2), tournament, round);
                unscheduledMatches.add(match);

                // Add the match again since the teams need to play with each other twice in a double rr
                if (isDoubleRoundRobin) {
                    unscheduledMatches.add(match);
                }
            }
        }

        Scheduler scheduler = Scheduler.build(tournamentTimeslots, unscheduledMatches);
        return scheduler.getMatches();
    }

    /**
     * Generates a list of matches for a bracket style tournament.
     * If there is a odd number of teams, then one team will win automatically by default. A match will still be generated for the
     * team but it will be automatically marked as compeleted with a result indicating that the team is the winner.
     * @param tournamentTimeslots if the number of timeslots are too few compared to the number of matches required to be played.
     * @param teams list of teams that need to be paired up and played against each other.
     * @return scheduled list of matches.
     * @throws MoreMatchesThanAvailableTimeslotsException if the number of timeslots are too few compared to the number of matches required to be played.
     * @throws ImperfectMatchingException if the scheduling failed because one or more matches are not assigned a timeslot.
     * @throws TournamentHasFinishedException if there is only 1 team.
     */
    public static List<Match> genBracketedMatches(List<TournamentTimeslot> tournamentTimeslots, List<Team> teams, Tournament tournament, Round round) throws MoreMatchesThanAvailableTimeslotsException, ImperfectMatchingException, TournamentHasFinishedException {
        int numTeams = teams.size();

        if (numTeams < 2) {
            throw new TournamentHasFinishedException();
        }

        boolean evenNumberOfTeams = numTeams % 2 == 0;

        List<Match> unscheduledMatches = new ArrayList<>();

        for (int i = 1; i < numTeams; i += 2) {
            Match match = Match.incompleteBuildWithTeamsOnly(List.of(teams.get(i-1), teams.get(i)), tournament, round);
            unscheduledMatches.add(match);
        }

        Scheduler scheduler = Scheduler.build(tournamentTimeslots, unscheduledMatches);
        List<Match> scheduledMatches = scheduler.getMatches();

        // Create a match with a winning result for the odd team, if any
        if (!evenNumberOfTeams) {
            Team oddTeam = teams.get(numTeams - 1);
            Result defaultedResult = Result
                    .builder()
                    .rankedTeam(List.of(oddTeam))
                    .build();
            Match defaultedMatch = Match.createDefaultedMatch(oddTeam, defaultedResult, tournament, round);
            defaultedResult.setMatch(defaultedMatch);
            scheduledMatches.add(defaultedMatch);
        }

        return scheduledMatches;
    }

    /**
     * Generates a list of matches for a KO style tournament.
     * If there is a odd number of teams, then one team will win automatically by default. A match will still be generated for the
     * team but it will be automatically marked as completed with a result indicating that the team is the winner.
     * @param tournamentTimeslots if the number of timeslots are too few compared to the number of matches required to be played.
     * @param teams list of teams that need to be paired up and played against each other.
     * @return scheduled list of matches.
     * @throws MoreMatchesThanAvailableTimeslotsException if the number of timeslots are too few compared to the number of matches required to be played.
     * @throws ImperfectMatchingException if the scheduling failed because one or more matches are not assigned a timeslot.
     */
    public static List<Match> genKOMatches(List<TournamentTimeslot> tournamentTimeslots, List<Team> teams, Tournament tournament, Round round) throws ImperfectMatchingException, MoreMatchesThanAvailableTimeslotsException {
        int numTeams = teams.size();
        // For recursion, if teams is less then two no match can be drawn return empty, if there is one remaining then they win auto and return that match.
        if (numTeams < 2) {
            List<Match> temp = new ArrayList<>();
            if (numTeams == 1) {
                Team oddTeam = teams.get(numTeams - 1);
                Result defaultedResult = Result
                        .builder()
                        .rankedTeam(List.of(oddTeam))
                        .build();
                Match defaultedMatch = Match.createDefaultedMatch(oddTeam, defaultedResult, tournament, round);
                defaultedResult.setMatch(defaultedMatch);
                temp.add(defaultedMatch);
            }
            return temp;
        }

        List<Match> unscheduledMatches = new ArrayList<>();

        // generates possible match between first team and everyone else
        for (int j = 1; j < numTeams; j++) {
            Team team1 = teams.get(0);
            Team team2 = teams.get(j);
            Match match = Match.incompleteBuildWithTeamsOnly(List.of(team1, team2), tournament, round);
            unscheduledMatches.add(match);
        }

        Scheduler scheduler = Scheduler.build(tournamentTimeslots, unscheduledMatches);

        List<Match> scheduledMatches = new ArrayList<>();
        scheduledMatches.addAll(scheduler.getKOMatches());

        int possibleMatches = scheduledMatches.size();

        if (possibleMatches == 0) {
            throw new ImperfectMatchingException("One or more matches are not scheduled a timeslot.");
        }

        // loop through possible matches seeing if solution can be comprised using this match
        for (int i = 0; i < possibleMatches; i++) {
            List<Team> tempTeams = teams;
            tempTeams.removeAll(scheduledMatches.get(i).getTeamsInMatch());
            try {
                // temp list to hold recursive solution
                List<Match> tempMatches = new ArrayList<>();
                tempMatches.addAll(genKOMatches(tournamentTimeslots, tempTeams, tournament, round));
                // this will potentially throw imperfect matching error meaning no solution could be drawn with remaining teams and given match


                List<Match> ret = new ArrayList<>();
                // add the match that had a solution to result
                ret.add(scheduledMatches.get(i));

                // add the recursive solution to result
                ret.addAll(tempMatches);

                return ret;
            } catch (ImperfectMatchingException ignore) {
                // wasn't able to find a solution for this match
                // going to try the next one if there are any, if not loop ends
            }
        }

        // wasn't able to find any matches that work with the first team actually having a match, no tournament can be drawn.
        throw new ImperfectMatchingException("Wasn't able to find any viable solutions");
    }


}
