package com.tov.futtor.torneo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tov.futtor.torneo.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByTournamentId(Long tournamentId);
}
