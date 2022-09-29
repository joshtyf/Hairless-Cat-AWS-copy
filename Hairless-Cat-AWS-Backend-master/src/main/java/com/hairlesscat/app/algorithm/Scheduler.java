package com.hairlesscat.app.algorithm;

import com.hairlesscat.app.match.Match;
import com.hairlesscat.app.princeton.Graph;
import com.hairlesscat.app.princeton.HopcroftKarp;
import com.hairlesscat.app.team.Team;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;

import java.util.*;

public class Scheduler {
    /**
     * A mapping that maps the index of a tournament timeslot in the graph to the actual timeslot.
     */
    private final Map<Integer, TournamentTimeslot> indexTournamentTimeslotsMapping;

    /**
     * A mapping that maps the index of a match in the graph to the actual match.
     */
    private final Map<Integer, Match> indexMatchMapping;

    private final int numOfMatchesToGenerate;

    /**
     * Basic graph data structure for the scheduling algorithm.
     * Should ideally be a bipartite graph after edges are added.
     */
    private final Graph baseGraph;

    /**
     * Graph as a result of the hopcroft karp algorithm.
     */
    private HopcroftKarp hopcroftKarp;

    private Scheduler(
            Map<Integer, TournamentTimeslot> indexTournamentTimeslotsMapping,
            Map<Integer, Match> indexMatchMapping,
            int numOfMatchesToGenerate,
            Graph baseGraph) {
        this.indexTournamentTimeslotsMapping = indexTournamentTimeslotsMapping;
        this.indexMatchMapping = indexMatchMapping;
        this.numOfMatchesToGenerate = numOfMatchesToGenerate;
        this.baseGraph = baseGraph;
    }

    /**
     * Builds a Scheduler object from the given tournament timeslots and required matches.
     * A Scheduler object should encapsulate a bipartite graph that is ready for the
     * Hopcroft Karp algorithm to be run.
     * @param tournamentTimeslots list of tournament timeslots that the matches can be played in.
     * @param matches list of matches to be assigned to tournament timeslots.
     * @return a Scheduler object that is ready for the Hopcroft Karp algorithm.
     * @throws MoreMatchesThanAvailableTimeslotsException if the number timeslots is lesser than the number of matches.
     */
    public static Scheduler build(List<TournamentTimeslot> tournamentTimeslots, List<Match> matches) throws MoreMatchesThanAvailableTimeslotsException {
        int numOfMatchesToGenerate = matches.size();
        int numOfTimeslots = tournamentTimeslots.size();

        if (numOfMatchesToGenerate > numOfTimeslots) {
            throw new MoreMatchesThanAvailableTimeslotsException(numOfMatchesToGenerate, numOfTimeslots);
        }

        Graph baseGraph = new Graph(numOfMatchesToGenerate + numOfTimeslots);

        // Index in graph <-> TournamentTimeslot
        Map<Integer, TournamentTimeslot> indexTournamentTimeslotsMapping = new HashMap<>();
        // Index in graph <-> Match
        Map<Integer, Match> indexMatchMapping = new HashMap<>();
        // Tournament <-> Index in graph
        Map<TournamentTimeslot, Integer> tournamentTimeslotsIndexMapping = new HashMap<>();
        // Match <-> Index in graph
        Map<Match, Integer> matchIndexMapping = new HashMap<>();

        int index = 0;

        for (Match match : matches) {
            indexMatchMapping.put(index, match);
            matchIndexMapping.put(match, index);
            index++;
        }

        for (TournamentTimeslot tournamentTimeslot : tournamentTimeslots) {
            indexTournamentTimeslotsMapping.put(index, tournamentTimeslot);
            tournamentTimeslotsIndexMapping.put(tournamentTimeslot, index);
            index++;
        }

        assert(index == numOfMatchesToGenerate + numOfTimeslots);

        createEdgesBetweenMatchesAndTimeslots(matches, tournamentTimeslotsIndexMapping, matchIndexMapping, baseGraph);

        return new Scheduler(indexTournamentTimeslotsMapping, indexMatchMapping, numOfMatchesToGenerate, baseGraph);
    }

    /**
     * Creates edges between match vertices and the tournament timeslot vertices in the graph.
     * An edge between a match m and a timeslot ts means that all teams playing in m
     * can play at ts.
     * @param matches list of matches.
     * @param tournamentTimeslotsIndexMapping a mapping of timeslots to their vertex indices in the graph.
     * @param matchIndexMapping a mapping of matches to their vertex indices in the graph.
     * @param baseGraph the graph to add edges to.
     */
    private static void createEdgesBetweenMatchesAndTimeslots(
            List<Match> matches,
            Map<TournamentTimeslot, Integer> tournamentTimeslotsIndexMapping,
            Map<Match, Integer> matchIndexMapping,
            Graph baseGraph) {

        for (Match match : matches) {
            Set<TournamentTimeslot> commonTimeslots = findCommonsTimeslotsBetweenTeams(match.getTeamsInMatch());
            int matchIndex = matchIndexMapping.get(match);
            for (TournamentTimeslot tournamentTimeslot : commonTimeslots) {
                int tournamentTimeslotIndex = tournamentTimeslotsIndexMapping.get(tournamentTimeslot);
                baseGraph.addEdge(matchIndex, tournamentTimeslotIndex);
            }
        }
    }

    /**
     * Finds all common timeslots between teams.
     * @param teams the list of teams.
     * @return a set of timeslots that is common between all the teams.
     */
    private static Set<TournamentTimeslot> findCommonsTimeslotsBetweenTeams(List<Team> teams) {
        Set<TournamentTimeslot> commonTimeslots = teams.get(0).getAvailabilities();
        for (int i = 1; i < teams.size(); i++) {
            commonTimeslots.retainAll(teams.get(i).getAvailabilities());
        }
        return commonTimeslots;
    }

    /**
     * Runs the hopcroft karp algorithm.
     */
    private void runHopcroftKarp() {
        if (this.hopcroftKarp == null) {
            this.hopcroftKarp = new HopcroftKarp(baseGraph);
        }
    }

    /**
     * Runs the hopcroft karp algorithm and returns
     * the list of matches, each assigned to a timeslot.
     * @return list of matches that are assigned to a timeslot.
     * @throws ImperfectMatchingException if the algorithm failed to assign one or more matches to a timeslot.
     */
    public List<Match> getMatches() throws ImperfectMatchingException {
        runHopcroftKarp();

        List<Match> matches = new ArrayList<>();
        for (int matchIndex = 0; matchIndex < numOfMatchesToGenerate; matchIndex++) {
            int timeslotIndex = this.hopcroftKarp.mate(matchIndex);
            if (timeslotIndex == -1) {
                throw new ImperfectMatchingException("One or more matches are not scheduled a timeslot.");
            }
            TournamentTimeslot matchedTimeslot = indexTournamentTimeslotsMapping.get(timeslotIndex);

            Match match = indexMatchMapping.get(matchIndex);
            match.setMatchStartTime(matchedTimeslot.getStartTime());
            match.setMatchEndTime(matchedTimeslot.getEndTime());
            matches.add(match);
        }
        return matches;
    }
    /**
     * Runs the hopcroft karp algorithm and returns
     * the list of matches, each assigned to a timeslot.
     * @return list of matches that are assigned to a timeslot.
     */
    public List<Match> getKOMatches() {
        runHopcroftKarp();

        List<Match> matches = new ArrayList<>();
        for (int matchIndex = 0; matchIndex < numOfMatchesToGenerate; matchIndex++) {
            int timeslotIndex = this.hopcroftKarp.mate(matchIndex);
            if (timeslotIndex != -1) {
                TournamentTimeslot matchedTimeslot = indexTournamentTimeslotsMapping.get(timeslotIndex);

                Match match = indexMatchMapping.get(matchIndex);
                match.setMatchStartTime(matchedTimeslot.getStartTime());
                match.setMatchEndTime(matchedTimeslot.getEndTime());
                matches.add(match);
            }
        }
        return matches;
    }
}
