package com.hairlesscat.app.tournament;

import com.hairlesscat.app.round.Round;
import com.hairlesscat.app.team.Team;
import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    @Autowired
    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public List<Tournament> getTournaments() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> getTournamentByTournamentId(Long tournamentId) {
        return tournamentRepository.findById(tournamentId);
    }

    public void deleteTournament(Long tournamentId) {
        tournamentRepository.deleteById(tournamentId);
    }

    public Tournament createTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public List<TournamentTimeslot> getAllTimeslotsFromTournament(Tournament tournament) {
        List<Round> rounds = tournament.getSchedule().getRounds();
        return rounds
                .stream()
                .flatMap(round -> round.getTimeslots().stream())
                .toList();
    }

    public List<Tournament> getTournamentsByUserId(String userId) {
        return tournamentRepository.findAllByAdminUser_UserId(userId);
    }


    public Tournament addTeamsToTournament(Tournament tournament, List<Team> teams) {
        for (Team t : teams) {
            tournament.getTeams().add(t);
            t.setTournament(tournament);
        }
        return tournamentRepository.save(tournament);
    }

    public Tournament deleteTeamFromTournament(Tournament tournament, Team team) {
        tournament.getTeams().remove(team);
        return tournamentRepository.save(tournament);
    }

    public boolean validateTeamInTournament(Tournament tournament, Team team) {
        return tournament.containsTeam(team);
    }

    public List<Tournament> filterForUnscheduledTournaments(List<Tournament> tournaments) {
        return tournaments
                .stream()
                .filter(tournament -> !tournament.getSchedule().isScheduled())
                .toList();
    }

    public List<Tournament> filterForScheduledTournaments(List<Tournament> tournaments) {
        return tournaments
                .stream()
                .filter(tournament -> tournament.getSchedule().isScheduled())
                .toList();
    }

    public void validateTeamsReadiness(Tournament tournament) throws TeamNotReadyException {
        Set<Team> teams = tournament.getTeams();
        int minNumberOfPlayers = tournament.getMinNumberOfPlayersPerTeam();
        int maxNumberOfPlayers = tournament.getMaxNumberOfPlayersPerTeam();
        String exceptionMessage = "";

        for (Team team : teams) {
            if (!team.hasIndicatedAvailability()) {
                exceptionMessage = String.format("Team %d has not indicated their availability", team.getTeamId());
                break;
            } else if (team.size() < minNumberOfPlayers) {
                exceptionMessage = String.format("Required number of players per team is %d but team %d only has %d players", minNumberOfPlayers, team.getTeamId(), team.size());
                break;
            } else if (team.size() > maxNumberOfPlayers) {
                exceptionMessage = String.format("Maximum number of players allowed is %d but team %d has %d players", maxNumberOfPlayers, team.getTeamId(), team.size());
                break;
            }
        }

        if (!exceptionMessage.isBlank()) {
            throw new TeamNotReadyException(exceptionMessage);
        }
    }

    public void validateTeamNumberRequirements(Tournament tournament) throws TooFewTeamsException, TooManyTeamsException {
        Set<Team> teams = tournament.getTeams();

		int numRequiredTeams = tournament.getRequiredNumberOfTeams();

        int numTeams = teams.size();

        if (numTeams < numRequiredTeams) {
            throw new TooFewTeamsException(numTeams, numRequiredTeams);
        } else if (numTeams > numRequiredTeams) {
            throw new TooManyTeamsException(numTeams, numRequiredTeams);
        }
    }
}
