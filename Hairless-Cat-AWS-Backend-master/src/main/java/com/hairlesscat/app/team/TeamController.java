package com.hairlesscat.app.team;

import com.fasterxml.jackson.annotation.JsonView;
import com.hairlesscat.app.match.MatchService;
import com.hairlesscat.app.result.ResultService;
import com.hairlesscat.app.schedule.ScheduleService;
import com.hairlesscat.app.teammember.TeamMember;
import com.hairlesscat.app.teammember.TeamMemberService;
import com.hairlesscat.app.tournament.Tournament;
import com.hairlesscat.app.tournament.TournamentService;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslotService;
import com.hairlesscat.app.user.User;
import com.hairlesscat.app.user.UserService;
import com.hairlesscat.app.util.ResponseWrapper;
import com.hairlesscat.app.validation.MissingFieldsException;
import com.hairlesscat.app.validation.Validator;
import com.hairlesscat.app.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/teams")
public class TeamController {
    private final TeamService teamService;
    private final UserService userService;
    private final TeamMemberService teamMemberService;
    private final ScheduleService scheduleService;
    private final TournamentTimeslotService tournamentTimeslotService;
    private final TournamentService tournamentService;
	private final MatchService matchService;
	private final ResultService resultService;

	@Autowired
    public TeamController(TeamService teamService,
                          UserService userService,
                          TeamMemberService teamMemberService,
                          ScheduleService scheduleService,
                          TournamentTimeslotService tournamentTimeslotService,
                          TournamentService tournamentService,
						  MatchService matchService,
						  ResultService resultService) {
        this.teamService = teamService;
        this.userService = userService;
        this.teamMemberService = teamMemberService;
        this.scheduleService = scheduleService;
        this.tournamentTimeslotService = tournamentTimeslotService;
        this.tournamentService = tournamentService;
		this.matchService = matchService;
		this.resultService = resultService;
    }

    @GetMapping()
    @JsonView(Views.TeamExtended.class)
    public Map<String, List<Team>> getTeams() {
        List<Team> teams = teamService.getTeams();
        return ResponseWrapper.wrapResponse("teams", teams);
    }

    @GetMapping(params = "user_id")
    @JsonView(Views.TeamExtended.class)
    public ResponseEntity<Map<String, List<Team>>> getTeamsByUser(@RequestParam(value = "user_id") String userId) {
        if (userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided user_id cannot be empty.");
        }

        User user = userService
                .getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with id " + userId));

        List<Team> teams = teamMemberService.findAllTeamsByUser(user);
        return ResponseEntity.ok(ResponseWrapper.wrapResponse("teams", teams));
    }

    @GetMapping(params = "tournament_id")
    @JsonView(Views.TeamExtended.class)
    public ResponseEntity<Map<String, List<Team>>> getTeamsByTournament(@RequestParam(value = "tournament_id") Long tournamentId) {
        Tournament tournament = tournamentService
                .getTournamentByTournamentId(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tournament found with id " + tournamentId));

        List<Team> teams = teamService.getTeamsOfTournament(tournament);

        return ResponseEntity.ok(ResponseWrapper.wrapResponse("teams", teams));
    }


    @GetMapping(path = "{team_id}")
    @JsonView(Views.TeamFull.class)
    public ResponseEntity<Team> getTeamById(@PathVariable("team_id") Long teamId) {
        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        return ResponseEntity.ok(team);
    }

    @GetMapping(path = "{team_id}/users")
    @JsonView(Views.UserSummary.class)
    public ResponseEntity<Map<String, List<User>>> getUsers(@PathVariable(value = "team_id") Long teamId) {
        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));
        List<User> users = teamService.getUsers(team);
        return ResponseEntity.ok(ResponseWrapper.wrapResponse("users", users));
    }

    @GetMapping(path = "{team_id}/tournamentId")
    public List<Long> getTournamentIdByTeamId(@PathVariable("team_id") Long teamId) {
        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        return List.of(team.getTournament().getTournamentId());
    }

    @PostMapping(path = "{team_id}/users")
    @JsonView(Views.TeamFull.class)
    public ResponseEntity<Team> addUsers(@PathVariable(value = "team_id") Long teamId,
                                         @RequestBody Map<String, String[]> requestBody) {
        List<User> users = new ArrayList<>();

        try {
            Validator.requestBodyTopLevelFieldValidation(List.of("user_ids"), requestBody.keySet());
        } catch (MissingFieldsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        for (String userId : requestBody.get("user_ids")) {
            userService.getUserById(userId).ifPresentOrElse(users::add, () -> {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with id " + userId);
            });
        }

        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        return ResponseEntity.ok(teamService.addUsers(team, users));
    }

    @DeleteMapping(path = "{team_id}")
    public void deleteTeam(@PathVariable("team_id") Long teamId) {
        teamService.deleteTeam(teamId);
    }

    @DeleteMapping(path = "{team_id}/users/{user_id}")
    @JsonView(Views.TeamFull.class)
    public ResponseEntity<Team> deleteUserFromTeam(@PathVariable("team_id") Long teamId,
                                                   @PathVariable("user_id") String userId) {
        User user = userService
                .getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with id " + userId));

        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        return ResponseEntity.ok(teamService.deleteUserFromTeam(team, user));
    }

    @GetMapping(path = "leader/{user_id}")
    @JsonView(Views.TeamExtended.class)
    public ResponseEntity<Map<String, List<Team>>> getAllTeamsOfLeader(@PathVariable(value = "user_id") String userId) {
        if (!userService.validateUser(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user exists with id " + userId);
        }

        List<Long> teamIds = teamMemberService.getAllTeamsOfLeader(userId);
        List<Team> teams = new ArrayList<>();
        teamIds
                .stream()
                .map(teamService::getTeamById)
                .map(optionalTeam -> optionalTeam
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Something went wrong with the database. Check with the administrator. Error: User is supposed to belong to a team but the team does not exist in the team database.")))
                .forEach(teams::add);

        return ResponseEntity.ok(ResponseWrapper.wrapResponse("teams", teams));
    }

    @GetMapping("{team_id}/member_availabilities/{user_id}")
    @JsonView(Views.Timeslot.class)
    public ResponseEntity<Map<String, List<TournamentTimeslot>>> getAllTimeslotIdsOfMember(@PathVariable(value = "team_id") Long teamId,
                                                                                           @PathVariable(value = "user_id") String userId) {
        if (!userService.validateUser(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with id " + userId);
        }
        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        if (!teamService.teamHasMember(team, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("User with user id %s not found in team with id %d", userId, teamId));
        }

        List<Long> timeslots = teamMemberService.findTimeslotIdsByUserIdTeamId(userId, teamId);
        List<TournamentTimeslot> tournamentTimeslots = new ArrayList<>();
        for (Long ts : timeslots) {
            TournamentTimeslot tts = tournamentTimeslotService.getTournamentTimeslotsById(ts)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id ." + teamId));

            tournamentTimeslots.add(tts);
        }

        return ResponseEntity.ok(ResponseWrapper.wrapResponse("timeslots", tournamentTimeslots));
    }

    @DeleteMapping(path = "{team_id}/member_availabilities/{user_id}")
    @JsonView(Views.TeamFull.class)
    public ResponseEntity<Team> deleteMemberAvailabilities(
            @PathVariable(value = "team_id") Long teamId,
            @PathVariable(value = "user_id") String userId) {

        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        TeamMember teamMember = teamMemberService.getTeamMemberByUserIdTeamId(userId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with id " + userId + " in team " + teamId));

        teamMemberService.deleteUserTimeslots(teamMember);
        return ResponseEntity.ok(team);

    }

    @PostMapping(path = "{team_id}/member_availabilities/{user_id}")
    public ResponseEntity<String> setMemberAvailabilities(
            @PathVariable(value = "team_id") Long teamId,
            @PathVariable(value = "user_id") String userId,
            @RequestBody Map<String, List<Long>> requestBody) {

        try {
            Validator.requestBodyTopLevelFieldValidation(List.of("timeslot_ids"), requestBody.keySet());
        } catch (MissingFieldsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        List<Long> tournamentTimeslotIds = requestBody.get("timeslot_ids");

        List<TournamentTimeslot> tournamentTimeslots;
        try {
            tournamentTimeslots = tournamentTimeslotService.findTimeslotsById(tournamentTimeslotIds);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body("One or more timeslot ids provided does not belong to any timeslots.");
        }


        if (!scheduleService.validateTimeslotsInTournament(team.getTournament(), tournamentTimeslots)) {
            return ResponseEntity
                    .badRequest()
                    .body("One or more timeslot ids provided do not correspond to the timeslots in tournament.");
        }

        User user = userService
                .getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with id " + userId));

        TeamMember teamMember = teamMemberService
                .getTeamMemberByUserAndTeam(user, team)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Team with id %d does not contain user with id %s", teamId, userId)));

        teamMemberService.setMemberAvailabilities(teamMember, tournamentTimeslotIds);
        return ResponseEntity.ok("Member availabilities set for user with id " + userId);
    }

    @GetMapping("{team_id}/team_availabilities")
    @JsonView(Views.Timeslot.class)
    public ResponseEntity<Map<String, List<TournamentTimeslot>>> getAllTimeslotsOfTeam(@PathVariable(value = "team_id") Long teamId) {
        Team team = teamService.getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        List<TournamentTimeslot> teamTimeslots = tournamentTimeslotService.getTimeslotsForTeam(team);

//		if (teamTimeslots.isEmpty()) {
//			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team has not indicated their availabilities.");
//		}
        // front end would rather have empty array instead of bad request

        return ResponseEntity.ok(ResponseWrapper.wrapResponse("timeslots", teamTimeslots));
    }

    @PostMapping(path = "actions/post_team_availabilities/{team_id}")
    public ResponseEntity<String> postTeamAvailabilities(
            @PathVariable(value = "team_id") Long teamId,
            @RequestBody Map<String, String> requestBody) {

        try {
            Validator.requestBodyTopLevelFieldValidation(List.of("user_id"), requestBody.keySet());
        } catch (MissingFieldsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        if (requestBody.get("user_id").isBlank()) {
            return ResponseEntity.badRequest().body("Must provide the user id of the user initiating the request.");
        }

        Team team = teamService
                .getTeamById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + teamId));

        TeamMember leader = teamService
                .getTeamLeader(team)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create availabilities for team. Team does not seem to have a team leader."));

        // Check if the leader is the one who sent this request.
        if (!leader.getUser().getUserId().equals(requestBody.get("user_id"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only team leaders are allowed to generate team availabilities!");
        }

        // Check if the team is indeed associated with a tournament
        Tournament tournament = team.getTournament();
        if (tournament == null) {
            return ResponseEntity.internalServerError().body("Something went wrong. Error: Team does not seem to be associated with any tournament.");
        }

        // Check if all members in the team has indicated their availabilities
        Set<TeamMember> teamMembers = team.getTeamMembers();
        if (!teamService.validateAllTeamMembersIndicatedAvailabilities(teamMembers)) {
            return ResponseEntity
                    .badRequest()
                    .body(String.format("Not all team members in team %s [team_id: %d] have indicated their availabilities!", team.getTeamName(), teamId));
        }

        int minNumberOfPlayersRequired = tournament.getTournamentParameter().getMinNumberOfPlayersPerTeam();

        if (teamMembers.size() < minNumberOfPlayersRequired) {
            return ResponseEntity.badRequest().body("Team size is less than the number of players required in the tournament.");
        }
        // Aggregate team member availabilities into team availabilities
        List<Long> tournamentTimeslotIds = teamService.aggregateTeamMemberAvailabilities(teamMembers, minNumberOfPlayersRequired);

        if (tournamentTimeslotIds.isEmpty()) {
            return ResponseEntity.badRequest().body("There are no common timings available for this team. Please retry again with another set of member availabilities");
        }

        List<TournamentTimeslot> tournamentTimeslots;
        try {
            tournamentTimeslots = tournamentTimeslotService.findTimeslotsById(tournamentTimeslotIds);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body("One or more timeslot ids provided does not belong to any timeslots.");
        }

        // Validate timeslots
        if (scheduleService.validateTimeslotsInTournament(tournament, tournamentTimeslots)) {
            // Add team availabilities to tournament
            tournamentTimeslotService.addTeamToTimeslots(team, tournamentTimeslots);
            teamService.saveTeamAvailabilityIndication(team);
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(String.format("One or more timeslot ids indicated by the team does not belong to tournament %s [tournament_id: %d]", tournament.getName(), tournament.getTournamentId()));
        }

        return ResponseEntity.ok("Team availabilities have been indicated");
    }

	@PostMapping(path = "{team_id}/confirm_result/{match_id}")
	public ResponseEntity<String> confirmResult(@PathVariable (value="team_id") Long team_id,
												@PathVariable (value="match_id") Long match_id) {

		Team team = teamService.getTeamById(team_id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + team_id));

		List<Long> team_ids =  teamService.getTeamsOfMatchId(match_id);
		if (!team_ids.contains(team.getTeamId())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No team found with id " + team_id + " in match " + match_id);
		}
		resultService.confirmByTeam(team_id);

		List<Long> resultNotConfirmedTeams = resultService.getTeamNotConfirmed(resultService.getResultByMatch(match_id).get(0).getResultId());

		if (resultNotConfirmedTeams.isEmpty()) {
			matchService.setMatchStatusOver(match_id);
			return ResponseEntity.ok("Specified team " + team_id + " has confirmed the result, " +
				"and now all teams of match " + match_id + " has confirmed it.");
		} else {
			return ResponseEntity.ok("Specified team " + team_id + " has confirmed the result, " +
				"but not all teams of match " + match_id + " has done it.");
		}
	}
}
