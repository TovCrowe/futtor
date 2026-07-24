package com.tov.futtor.torneo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tov.futtor.torneo.entity.Team;
import org.springframework.data.jpa.repository.Query;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT t FROM Team t WHERE t.tournament.id = :tournamentId")
    List<Team> findByTournamentId(Long tournamentId);

    boolean existsByTournamentIdAndNameIgnoreCase(Long tournamentId, String name);

    boolean existsByTournamentIdAndNameIgnoreCaseAndIdNot(Long tournamentId, String name, Long id);
}
