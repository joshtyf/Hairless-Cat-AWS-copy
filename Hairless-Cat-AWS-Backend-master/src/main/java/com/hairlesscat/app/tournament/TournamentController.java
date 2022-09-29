package com.hairlesscat.app.tournament;

import com.fasterxml.jackson.annotation.JsonView;
import com.hairlesscat.app.algorithm.ImperfectMatchingException;
import com.hairlesscat.app.algorithm.MoreMatchesThanAvailableTimeslotsException;
import com.hairlesscat.app.algorithm.TournamentHasFinishedException;
import com.hairlesscat.app.match.Match;
import com.hairlesscat.app.round.Round;
import com.hairlesscat.app.round.RoundService;
import com.hairlesscat.app.schedule.Schedule;
import com.hairlesscat.app.schedule.ScheduleService;
import com.hairlesscat.app.team.Team;
import com.hairlesscat.app.team.TeamService;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslotService;
import com.hairlesscat.app.user.User;
import com.hairlesscat.app.user.UserService;
import com.hairlesscat.app.util.ResponseWrapper;
import com.hairlesscat.app.validation.MissingFieldsException;
import com.hairlesscat.app.validation.Validator;
import com.hairlesscat.app.view.Views;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/tournaments")
public class TournamentController {
    private final TournamentService tournamentService;
    private final ScheduleService scheduleService;
    private final TournamentTimeslotService tournamentTimeslotService;
    private final TeamService teamService;
    private final UserService userService;
  	private final RoundService roundService;

	public TournamentController(TournamentService tournamentService, ScheduleService scheduleService, TournamentTimeslotService tournamentTimeslotService, TeamService teamService, UserService userService, RoundService roundService) {
        this.tournamentService = tournamentService;
        this.scheduleService = scheduleService;
        this.tournamentTimeslotService = tournamentTimeslotService;
        this.teamService = teamService;
        this.userService = userService;
		    this.roundService = roundService;
    }

    @GetMapping
    @JsonView(Views.TournamentSummary.class)
    public ResponseEntity<Map<String, List<Tournament>>> getTournaments(
            @RequestParam(value = "user_id", required = false) String userId,
            @RequestParam(value = "filter", required = false) Optional<String> filter) {

        List<Tournament> tournaments;

        if (userId == null) {
            tournaments = tournamentService.getTournaments();
        } else {
            tournaments = tournamentService.getTournamentsByUserId(userId);
        }

        if (filter.isPresent()) {
            try {
                TournamentFilter tournamentFilter = TournamentFilter.valueOf(filter.get().toUpperCase());
                switch (tournamentFilter) {
                    case SCHEDULED -> tournaments = tournamentService.filterForScheduledTournaments(tournaments);
                    case UNSCHEDULED -> tournaments = tournamentService.filterForUnscheduledTournaments(tournaments);
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("'%s' is not a valid filter.", filter.get()));
            }
        }

        return ResponseEntity.ok(ResponseWrapper.wrapResponse("tournaments", tournaments));
    }

    @GetMapping(path = "{tournament_id}")
    @JsonView(Views.TournamentFull.class)
    public ResponseEntity<Tournament> getTournamentById(@PathVariable(value = "tournament_id") Long tournamentId) {
        Tournament tournament = tournamentService
                .getTournamentByTournamentId(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tournament found with id " + tournamentId));
        return ResponseEntity.ok(tournament);
    }

    @DeleteMapping(params = {"tournament_id"})
    public Long deleteTournament(@RequestParam(value = "tournament_id") Long tournamentId) {
        try {
            tournamentService.deleteTournament(tournamentId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return tournamentId;
    }

    @PostMapping
    //TODO: validation error handling
    @JsonView(Views.TournamentFull.class)
    public ResponseEntity<Tournament> createTournament(
            @RequestParam(value = "user_id", required = false) Optional<String> optionalUserId,
            @Valid @RequestBody Tournament tournament) {

        optionalUserId.ifPresent(userId -> {
            if (userId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided user_id must not be empty");
            }
            User user = userService
                    .getUserById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot create tournament because no user found with id " + userId));
            tournament.setAdminUser(user);
        });
        Schedule schedule = tournament.getSchedule();
        List<TournamentTimeslot> timeslots = tournamentTimeslotService.generateThirtyMinuteTimeslotsFromStartAndEndTime(schedule.getTournamentStartTime(), schedule.getTournamentEndTime());
		timeslots.forEach(timeslot -> timeslot.setTournament(tournament));
        List<Round> rounds = roundService.createRounds(tournament, schedule, timeslots);
        scheduleService.addRoundsToSchedule(schedule, rounds);

		Tournament savedTournament = tournamentService.createTournament(tournament);
        return ResponseEntity.ok(savedTournament);
    }


    @GetMapping(path = "{tournament_id}/timeslots")
    @JsonView(Views.TournamentTimeslot.class)
    public ResponseEntity<Map<String, Object>> getAllTimeslotsByTournamentId(@PathVariable(value = "tournament_id") Long tournamentId) {
        Tournament tournament = tournamentService
                .getTournamentByTournamentId(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tournament found with id " + tournamentId));
        List<TournamentTimeslot> timeslots = tournamentService.getAllTimeslotsFromTournament(tournament);
        Map<String, Object> body = new HashMap<>();
        body.put("tournament_id", tournamentId);
        body.put("timeslots", timeslots);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping(path = "{tournament_id}/teams/{team_id}")
    @JsonView(Views.TournamentFull.class)
    public ResponseEntity<Tournament> deleteTeamById(@PathVariable(value = "tournament_id") Long tournamentId, @PathVariable(value = "team_id") Long teamId) {
        Tournament tournament = tournamentService
                .getTournamentByTournamentId(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tournament found with id " + tournamentId));

        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        return ResponseEntity.ok(tournamentService.deleteTeamFromTournament(tournament, team));
    }

    private void generateRRMatchSchedule(Tournament tournament) {

        Schedule schedule = tournament.getSchedule();

        Set<Team> teams = tournament.getTeams();

        // The one and only round in the round robin tournament
        Round round = tournament.getSchedule().getRounds().get(0);

        try {
            List<Match> matches = scheduleService.generateRRMatches(schedule, List.copyOf(teams), tournament);
            roundService.addMatchesToRound(round, matches);
            scheduleService.setScheduleSuccess(schedule);
        } catch (ImperfectMatchingException | MoreMatchesThanAvailableTimeslotsException e) {
            scheduleService.setScheduleError(schedule, e.getMessage());
            roundService.resetTeamAvailabilitiesFor(round);
        }
    }

    private void generateBracketMatchSchedule(Tournament tournament) {
        Schedule schedule = tournament.getSchedule();
        int currentRoundNumber = schedule.getCurrentRound();
        Round currentRound = schedule.getRounds().get(currentRoundNumber);

        List<Team> teams;
        if (currentRoundNumber == 0) { // first round
            teams = tournament.getTeams().stream().toList();
        } else {
            Round previousRound = tournament.getSchedule().getRounds().get(currentRoundNumber - 1);
            teams = previousRound
                    .getMatches()
                    .stream()
                    .map(match -> match.getResult().getRankedTeam().get(0)) // get winners of previous round
                    .toList();
        }

        List<TournamentTimeslot> tournamentTimeslots = currentRound.getTimeslots();

        try {
            List<Match> matches = scheduleService.generateBracketMatches(schedule, tournament, teams, tournamentTimeslots);
            roundService.addMatchesToRound(currentRound, matches);
            scheduleService.setScheduleSuccess(tournament.getSchedule());
        } catch (ImperfectMatchingException | MoreMatchesThanAvailableTimeslotsException | TournamentHasFinishedException e) {
            scheduleService.setScheduleError(tournament.getSchedule(), e.getMessage());
            roundService.resetTeamAvailabilitiesFor(currentRound);
        }
    }

    private void generateKOMatchSchedule(Tournament tournament) {
        Schedule schedule = tournament.getSchedule();
        int currentRoundNumber = schedule.getCurrentRound();
        Round currentRound = schedule.getRounds().get(currentRoundNumber);

        List<Team> teams;
        if (currentRoundNumber == 0) { // first round
            teams = tournament.getTeams().stream().toList();
        } else {
            Round previousRound = tournament.getSchedule().getRounds().get(currentRoundNumber - 1);
            teams = previousRound
                    .getMatches()
                    .stream()
                    .map(match -> match.getResult().getRankedTeam().get(0)) // get winners of previous round
                    .toList();
        }

        List<TournamentTimeslot> tournamentTimeslots = currentRound.getTimeslots();

        try {
            if (teams.size() < 2) {
                throw new TournamentHasFinishedException("Not enough teams left in tournament to make a schedule.");
            }
            List<Match> matches = scheduleService.generateKOMatches(schedule, tournament, teams, tournamentTimeslots);
            roundService.addMatchesToRound(currentRound, matches);
            scheduleService.setScheduleSuccess(tournament.getSchedule());
        } catch (ImperfectMatchingException | MoreMatchesThanAvailableTimeslotsException | TournamentHasFinishedException e) {
            scheduleService.setScheduleError(tournament.getSchedule(), e.getMessage());
            roundService.resetTeamAvailabilitiesFor(currentRound);
        }
    }

    @PostMapping(path = "actions/gen_match_schedule/{tournament_id}")
    public ResponseEntity<String> generateMatchSchedule(@PathVariable(value = "tournament_id") Long tournamentId) {
        Tournament tournament = tournamentService
                .getTournamentByTournamentId(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tournament found with id " + tournamentId));

        String errorMsgPrefix = String.format(
                "Unable to generate schedule for tournament with id %d. Reason: ",
                tournament.getTournamentId());

        try {
            tournamentService.validateTeamNumberRequirements(tournament);
            tournamentService.validateTeamsReadiness(tournament);
        } catch (TooManyTeamsException | TooFewTeamsException | TeamNotReadyException e) {
            return ResponseEntity.badRequest().body(errorMsgPrefix + e.getMessage());
        }

        TournamentStyle tournamentStyle = tournament.getTournamentStyle();

        switch (tournamentStyle) {
            case SINGLE_ROUND_ROBIN, DOUBLE_ROUND_ROBIN -> generateRRMatchSchedule(tournament);
            case BRACKET -> generateBracketMatchSchedule(tournament);
            case SINGLE_KO -> generateKOMatchSchedule(tournament);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Running tournament scheduling algorithm.");
    }

    @PostMapping(path = "{tournament_id}/teams")
    @JsonView(Views.TeamFull.class)
    public ResponseEntity<Team> addTeamToTournament(
            @PathVariable("tournament_id") Long tournamentId,
            @RequestBody Map<String, String> requestBody) {

        try {
            Validator.requestBodyTopLevelFieldValidation(List.of("user_id", "team_name"), requestBody.keySet());
        } catch (MissingFieldsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        String userId = requestBody.get("user_id");
        String teamName = requestBody.get("team_name");

        User user = userService
                .getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot create a team because no user is found with id " + userId));

        Tournament tournament = tournamentService
                .getTournamentByTournamentId(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot create a team because no tournament is found with id " + tournamentId));

        return ResponseEntity.ok(teamService.createTeam(tournament, teamName, user));
    }
}
