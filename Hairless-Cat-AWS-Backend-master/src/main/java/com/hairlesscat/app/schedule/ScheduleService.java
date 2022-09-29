package com.hairlesscat.app.schedule;

import com.hairlesscat.app.algorithm.Algorithm;
import com.hairlesscat.app.algorithm.ImperfectMatchingException;
import com.hairlesscat.app.algorithm.MoreMatchesThanAvailableTimeslotsException;
import com.hairlesscat.app.algorithm.TournamentHasFinishedException;
import com.hairlesscat.app.match.Match;
import com.hairlesscat.app.round.Round;
import com.hairlesscat.app.team.Team;
import com.hairlesscat.app.tournament.Tournament;
import com.hairlesscat.app.tournament.TournamentStyle;
import com.hairlesscat.app.tournamenttimeslot.Timeslot;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public boolean validateTimeslotsInTournament(Tournament tournament, List<TournamentTimeslot> tournamentTimeslots) {
        return tournamentTimeslots
                .stream()
                .allMatch(tournamentTimeslot -> tournamentTimeslot.getTournament().equals(tournament));
    }

    public List<Match> generateRRMatches(
            Schedule schedule,
            List<Team> teams,
            Tournament tournament) throws ImperfectMatchingException, MoreMatchesThanAvailableTimeslotsException {

        Round round = schedule.getRounds().get(0);
        List<Match> matches = Algorithm.genRRMatches(
                schedule.getRounds().get(0).getTimeslots(), // get timeslots from the only round in a RR tournament
                teams,
                tournament.getTournamentStyle() == TournamentStyle.DOUBLE_ROUND_ROBIN,
                tournament,
                round);

        // Algorithm.genRRMatches does not add tournament and round reference
        // We should probably refactor this so that we don't forget to add
        // tournament reference in future developments.
//        matches.forEach(match -> {
//            match.setTournament(tournament);
//            match.setRound(round);
//        });

        return matches;
    }

    public List<Match> generateBracketMatches(Schedule schedule,Tournament tournament, List<Team> teams, List<TournamentTimeslot> tournamentTimeslots) throws MoreMatchesThanAvailableTimeslotsException, ImperfectMatchingException, TournamentHasFinishedException {
        Round round = schedule.getRounds().get(schedule.getCurrentRound());
        List<Match> matches = Algorithm.genBracketedMatches(
                round.getTimeslots(), // get timeslots from the only round in a RR tournament
                teams,
                tournament,
                round);

        // Algorithm.genRRMatches does not add tournament and round reference
        // We should probably refactor this so that we don't forget to add
        // tournament reference in future developments.
//        matches.forEach(match -> {
//            match.setTournament(tournament);
//            match.setRound(round);
//        });

        return matches;
    }

    public List<Match> generateKOMatches(Schedule schedule,Tournament tournament, List<Team> teams, List<TournamentTimeslot> tournamentTimeslots) throws MoreMatchesThanAvailableTimeslotsException, ImperfectMatchingException, TournamentHasFinishedException {
        Round round = schedule.getRounds().get(schedule.getCurrentRound());
        List<Match> matches = Algorithm.genKOMatches(
                round.getTimeslots(), // get timeslots from the only round in a RR tournament
                teams,
                tournament,
                round);

        // Algorithm.genRRMatches does not add tournament and round reference
        // We should probably refactor this so that we don't forget to add
        // tournament reference in future developments.
//        matches.forEach(match -> {
//            match.setTournament(tournament);
//            match.setRound(round);
//        });

        return matches;
    }

    public void setScheduleError(Schedule schedule, String errorMsg) {
        schedule.setScheduleStatusError(errorMsg);
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void setScheduleSuccess(Schedule schedule) {
        schedule.setScheduleStatusSuccess();
    }

    public List<Match> getAllMatchesFrom(Schedule schedule) throws ScheduleNotStartedException, ScheduleErrorException {
        if (schedule.hasNotStartedScheduling()) {
            throw new ScheduleNotStartedException();
        } else if (schedule.hasScheduleError()) {
            throw new ScheduleErrorException(schedule.getScheduleStatusErrorMessage());
        } else {
            return schedule
                    .getRounds()
                    .stream()
                    .flatMap(round -> round.getMatches().stream())
                    .toList();
        }
    }

    public void addRoundsToSchedule(Schedule schedule, List<Round> rounds) {
        rounds.forEach(round -> schedule.getRounds().add(round));
    }

    public Optional<Integer> getCurrentRoundOfTournament(Long tournament_id) {
        return scheduleRepository.findByTournament_TournamentId(tournament_id).map(Schedule::getCurrentRound);
    }
}
