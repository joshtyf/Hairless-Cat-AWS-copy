package com.hairlesscat.app.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    Optional<Schedule> findByTournament_TournamentId(Long tournamentId);
}
