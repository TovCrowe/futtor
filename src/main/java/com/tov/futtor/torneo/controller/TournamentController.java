package com.tov.futtor.torneo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tov.futtor.torneo.TournamentService;
import com.tov.futtor.torneo.dto.CreateMatchRequest;
import com.tov.futtor.torneo.dto.StandingsRowDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;


    @GetMapping("/{tournamentId}/standings")
    public ResponseEntity<List<StandingsRowDto>> getStandings(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.calculateStandings(tournamentId));
    }

    @PostMapping("/{tournamentId}/matches")
    public ResponseEntity<CreateMatchRequest> createMatch(@PathVariable Long tournamentId, @RequestBody @Valid CreateMatchRequest request) {
        log.info("Creating match for tournament {}: {} vs {} ({}-{})", tournamentId, request.getHomeTeamId(), request.getAwayTeamId(), request.getHomeTeamGoals(), request.getAwayTeamGoals());
        tournamentService.createMatch(tournamentId, request);
        return ResponseEntity.ok(request);
    }
}
