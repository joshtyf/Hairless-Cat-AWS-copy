package com.hairlesscat.app.teammember;

import com.hairlesscat.app.team.Team;
import com.hairlesscat.app.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;

    @Autowired
    public TeamMemberService(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    public List<Team> findAllTeamsByUser(User user) {
        return teamMemberRepository
                .findALlByUser(user)
                .stream()
                .map(TeamMember::getTeam)
                .toList();
    }

    public List<Long> getAllTeamsOfLeader(String userId) {
        return teamMemberRepository.getAllTeamsOfLeader(userId);
    }

	public Optional<TeamMember> getTeamMemberByUserIdTeamId(String userId, Long teamId) {
		return teamMemberRepository.findByIds(userId, teamId);
	}

    public List<Long> findTimeslotIdsByUserIdTeamId(String userId, Long teamId) {
        return teamMemberRepository.findTimeslotIdsByUserIdTeamId(userId, teamId);
    }

    @Transactional
    public void setMemberAvailabilities(TeamMember teamMember, List<Long> timeslotIds) {
        teamMember.setUserIndicatedTimeslotIds(Set.copyOf(timeslotIds));
        teamMember.setIndicatedAvailabilities(true);
    }

	@Transactional
	public void deleteUserTimeslots(TeamMember teamMember) {
		Set<Long> timeslotIds = new HashSet<>();
		teamMember.setUserIndicatedTimeslotIds(timeslotIds);
		teamMember.setIndicatedAvailabilities(false);
	}

    public Optional<TeamMember> getTeamMemberByUserAndTeam(User user, Team team) {
        return teamMemberRepository.findByUserAndTeam(user, team);
    }
}
