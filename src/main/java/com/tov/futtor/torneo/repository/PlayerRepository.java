package com.tov.futtor.torneo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tov.futtor.torneo.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamId(Long teamId);

    // Un jugador conoce su torneo a través de su equipo: Player -> team -> tournament
    List<Player> findByTeamTournamentId(Long tournamentId);
}
