package com.hairlesscat.app.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends  JpaRepository<Team, Long>{

	@Query(value = "SELECT * FROM team WHERE tournament_id = ?1", nativeQuery = true)
	List<Team> findAllByTournament(Long tournament);

	@Query(value = "SELECT team_id FROM match_team_status_map WHERE match_id = ?1", nativeQuery = true)
	List<Long> findTeamsByMatchId(Long match_id);

}
