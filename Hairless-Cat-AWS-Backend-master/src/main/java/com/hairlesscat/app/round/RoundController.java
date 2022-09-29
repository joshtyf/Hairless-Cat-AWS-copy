package com.hairlesscat.app.round;

import com.fasterxml.jackson.annotation.JsonView;
import com.hairlesscat.app.schedule.ScheduleService;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;
import com.hairlesscat.app.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rounds")
public class RoundController {

    private final RoundService roundService;
    private final ScheduleService scheduleService;

    @Autowired
    public RoundController(RoundService roundService, ScheduleService scheduleService) {
        this.roundService = roundService;
        this.scheduleService = scheduleService;
    }

    @GetMapping()
    @JsonView(Views.ResultSummary.class)
    public Map<String, List<Round>> getRounds() {
        List<Round> rounds = roundService.getRounds();
        Map<String, List<Round>> round = new HashMap<>();
        round.put("rounds", rounds);
        return round;
    }

    @GetMapping(path = "/current_round/{tournament_id}")
    @JsonView(Views.RoundSummary.class)
    public ResponseEntity<Integer> getCurrentRound(@PathVariable(value = "tournament_id") Long tournament_id) {
        Integer currentRound = scheduleService
                .getCurrentRoundOfTournament(tournament_id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No current round with the tournament id " + tournament_id));

        return ResponseEntity.ok(currentRound);
    }

    @GetMapping(path = "{round_number}/timeslots/{tournament_id}")
    @JsonView(Views.RoundSummary.class)
    public ResponseEntity<List<TournamentTimeslot>> getTimeslotsOfRound
            (@PathVariable(value = "round_number") Integer round_number,
             @PathVariable(value = "tournament_id") Long tournament_id) {
        List<TournamentTimeslot> tournamentTimeslot = roundService.getTimeslotsOfRound(round_number, tournament_id);
        return ResponseEntity.ok(tournamentTimeslot);
    }

}


