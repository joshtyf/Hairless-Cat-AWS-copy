package com.hairlesscat.app.round;

import com.hairlesscat.app.tournamenttimeslot.TournamentTimeslot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {

	@Query(value = "SELECT * FROM tournament_timeslot NATURAL JOIN (SELECT tournament_timeslot_id FROM round_timeslot_per_round_map as rtpm NATURAL JOIN" +
		"(SELECT round_id FROM round as r WHERE r.tournament_id = 2) as temp WHERE rtpm.round_number = 1) as temp2", nativeQuery = true)
	List<TournamentTimeslot> findRoundTimeslotsByTournamentId(Integer round_number, Long tournament_id);
}
