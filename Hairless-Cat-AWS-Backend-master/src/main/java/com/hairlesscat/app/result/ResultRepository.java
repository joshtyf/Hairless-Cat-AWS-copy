package com.hairlesscat.app.result;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long>{

	@Query(value = "SELECT * FROM result WHERE result_match_id = ?1", nativeQuery = true)
	List<Result> findByMatch(Long mid);

	@Modifying
	@Transactional
	@Query(value = "UPDATE result_team_confirmation_map SET team_confirmation = ?1 WHERE team_id = ?2", nativeQuery = true)
	void confirmByTeam(Boolean confirm, Long team_id);

	@Modifying
	@Transactional
	@Query(value = "INSERT INTO result_team_confirmation_map (team_confirmation, team_id, result_id) VALUES (?1, ?2, ?3)", nativeQuery = true)
	void setDefaultConfirmation(Boolean confirm_default, Long team_id, Long result_id);

	@Query(value = "SELECT result_id FROM result_team_confirmation_map WHERE team_confirmation = ?1 AND result_id = ?2", nativeQuery = true)
	List<Long> findFalseTeams(boolean b_false, Long result_id);

	@Query(value = "SELECT team_id FROM result_team_confirmation_map WHERE team_confirmation = ?1", nativeQuery = true)
	List<Long> findFalseTeams(boolean b_false);
}

