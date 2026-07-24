package com.tov.futtor.torneo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tov.futtor.torneo.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTournamentId(Long tournamentId);
}
