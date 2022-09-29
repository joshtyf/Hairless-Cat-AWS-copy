package com.hairlesscat.app.round;

import com.hairlesscat.app.match.Match;
import com.hairlesscat.app.schedule.Schedule;
import com.hairlesscat.app.tournament.Tournament;
import com.hairlesscat.app.tournament.TournamentStyle;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class RoundService {

    private final RoundRepository roundRepository;

    @Autowired
    public RoundService(RoundRepository roundRepository) {
        this.roundRepository = roundRepository;
    }

    @Transactional
    public void addMatchesToRound(Round round, List<Match> matches) {
        matches.forEach(match -> round.getMatches().add(match));
    }

    public List<Round> getRounds() {
        return roundRepository.findAll();
    }

    public List<Round> createRounds(Tournament tournament, Schedule schedule, List<TournamentTimeslot> timeslots) {
        TournamentStyle style = tournament.getTournamentStyle();
        int numTeams = tournament.getRequiredNumberOfTeams();

        int numRounds;
        if (style == TournamentStyle.BRACKET) {
            // TODO: check with andrew and leah if they are on the same page on how bracket tournaments work
            numRounds = (int) Math.round(Math.ceil(((double) Math.log(numTeams) / Math.log(2.0))));
        } else { // Round-robin
            numRounds = 1;
        }

        int totalNumOfTimeslots = timeslots.size();
        int numTimeslotPerRound = totalNumOfTimeslots / numRounds;

        Queue<TournamentTimeslot> timeslotQueue = new PriorityQueue<>(timeslots);

        List<Round> rounds = new ArrayList<>();
        for (int i = 0; i < numRounds; i++) {
            Round round = Round.builder().tournament(tournament).schedule(schedule).build();
            List<TournamentTimeslot> timeslotsForRound = new ArrayList<>(numTimeslotPerRound);
            for (int j = 0; j < numTimeslotPerRound; j++) {
                // is the calculation correct? what if we poll() and get null?
                TournamentTimeslot tournamentTimeslot = timeslotQueue.poll();
                assert tournamentTimeslot != null;
                tournamentTimeslot.setRound(round);
                timeslotsForRound.add(tournamentTimeslot);
            }
            round.setTimeslots(timeslotsForRound);
            rounds.add(round);
        }
        return rounds;
    }

    public List<TournamentTimeslot> getTimeslotsOfRound(Integer round_number, Long tournament_id) {
        return roundRepository.findRoundTimeslotsByTournamentId(round_number, tournament_id);
    }

    @Transactional
    public void resetTeamAvailabilitiesFor(Round round) {
        round
                .getTimeslots()
                .forEach(tournamentTimeslot -> tournamentTimeslot.setAvailableTeams(new HashSet<>()));
    }
}
