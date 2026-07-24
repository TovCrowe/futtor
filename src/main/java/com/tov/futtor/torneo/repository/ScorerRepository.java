package com.tov.futtor.torneo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tov.futtor.torneo.entity.Scorer;

public interface ScorerRepository extends JpaRepository<Scorer, Long> {

    List<Scorer> findByMatchId(Long matchId);

    // Goleo de todo el torneo: Scorer -> match -> tournament
    List<Scorer> findByMatchTournamentId(Long tournamentId);

    void deleteByMatchId(Long matchId);
}
