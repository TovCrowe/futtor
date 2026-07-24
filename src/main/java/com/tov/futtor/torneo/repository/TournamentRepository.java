package com.tov.futtor.torneo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tov.futtor.torneo.entity.Tournament;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

}

