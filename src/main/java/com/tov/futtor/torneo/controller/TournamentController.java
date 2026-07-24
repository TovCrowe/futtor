package com.tov.futtor.torneo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tov.futtor.torneo.TournamentService;
import com.tov.futtor.torneo.dto.CreatePlayerRequest;
import com.tov.futtor.torneo.dto.CreateTeamRequest;
import com.tov.futtor.torneo.dto.CreateTournamentRequest;
import com.tov.futtor.torneo.dto.MatchDTO;
import com.tov.futtor.torneo.dto.PlayerDto;
import com.tov.futtor.torneo.dto.ScorerDto;
import com.tov.futtor.torneo.dto.StandingsRowDto;
import com.tov.futtor.torneo.dto.TournamentDto;
import com.tov.futtor.torneo.dto.UpdateMatchRequest;
import com.tov.futtor.torneo.dto.UpdatePlayerRequest;
import com.tov.futtor.torneo.dto.UpdateScorersRequest;
import com.tov.futtor.torneo.dto.UpdateTeamRequest;
import com.tov.futtor.torneo.entity.Tournament;

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

    @PostMapping("/{tournamentId}/teams")
    public ResponseEntity<Void> createTeam(@PathVariable Long tournamentId, @RequestBody @Valid CreateTeamRequest teamRequest) {
        log.info("Creating team for tournament {}: {}", tournamentId, teamRequest.getName());
        tournamentService.createTeam(tournamentId, teamRequest);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{tournamentId}/teams/{teamId}")
    public ResponseEntity<Void> updateTeam(@PathVariable Long tournamentId, @PathVariable Long teamId,
            @RequestBody @Valid UpdateTeamRequest teamRequest) {
        log.info("Updating team {} for tournament {}: {}", teamId, tournamentId, teamRequest.getName());
        tournamentService.updateTeam(tournamentId, teamId, teamRequest);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{tournamentId}/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long tournamentId, @PathVariable Long teamId) {
        log.info("Deleting team {} for tournament {}", teamId, tournamentId);
        tournamentService.deleteTeam(tournamentId, teamId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tournamentId}/generate-matches")
    public ResponseEntity<Void> generateMatches(@PathVariable Long tournamentId) {
        log.info("Generating matches for tournament {}", tournamentId);
        tournamentService.createMatchGamesAutomatically(tournamentId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{tournamentId}/{matchId}/update-match")
    public ResponseEntity<Void> updateMatch(@PathVariable Long tournamentId, @PathVariable Long matchId, @RequestBody @Valid UpdateMatchRequest matchRequest) {
        tournamentService.updateMatch(tournamentId, matchId, matchRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create-tournament")
    public ResponseEntity<Long> createTournament(@RequestBody @Valid CreateTournamentRequest request) {
        Tournament tournament = tournamentService.createTournament(request);
        return ResponseEntity.ok(tournament.getId());
    }

    @GetMapping("/{tournamentId}/matches")
    public ResponseEntity<List<MatchDTO>> getMatches(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.getMatches(tournamentId));
    }

    @GetMapping
    public ResponseEntity<List<TournamentDto>> getTournaments() {
        return ResponseEntity.ok(tournamentService.getTournaments());
    }

    @PostMapping("/{tournamentId}/teams/{teamId}/players")
    public ResponseEntity<Void> addPlayerToTeam(@PathVariable Long tournamentId, @PathVariable Long teamId,
            @RequestBody @Valid CreatePlayerRequest request) {
        log.info("Adding player {} to team {} in tournament {}", request.getName(), teamId, tournamentId);
        tournamentService.addPlayerToTeam(tournamentId, teamId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{tournamentId}/teams/{teamId}/players")
    public ResponseEntity<List<PlayerDto>> getPlayersByTeam(@PathVariable Long tournamentId, @PathVariable Long teamId) {
        return ResponseEntity.ok(tournamentService.getPlayersByTeam(teamId));
    }

    @GetMapping("/{tournamentId}/teams/{teamId}/players/{playerId}")
    public ResponseEntity<PlayerDto> getPlayerById(@PathVariable Long tournamentId, @PathVariable Long teamId,
            @PathVariable Long playerId) {
        return ResponseEntity.ok(tournamentService.getPlayerById(playerId));
    }

    @PutMapping("/{tournamentId}/teams/{teamId}/players/{playerId}")
    public ResponseEntity<Void> updatePlayer(@PathVariable Long tournamentId, @PathVariable Long teamId,
            @PathVariable Long playerId, @RequestBody @Valid UpdatePlayerRequest request) {
        log.info("Updating player {} in team {} for tournament {}", playerId, teamId, tournamentId);
        tournamentService.editPlayer(tournamentId, teamId, playerId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{tournamentId}/teams/{teamId}/players/{playerId}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long tournamentId, @PathVariable Long teamId,
            @PathVariable Long playerId) {
        log.info("Deleting player {} from team {} in tournament {}", playerId, teamId, tournamentId);
        tournamentService.deletePlayer(tournamentId, teamId, playerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{tournamentId}/top-scorers")
    public ResponseEntity<List<ScorerDto>> getTopScorers(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(tournamentService.getTopScorersPerTournament(tournamentId));
    }

    @GetMapping("/{tournamentId}/matches/{matchId}/scorers")
    public ResponseEntity<List<ScorerDto>> getMatchScorers(@PathVariable Long tournamentId,
            @PathVariable Long matchId) {
        return ResponseEntity.ok(tournamentService.getMatchScorers(tournamentId, matchId));
    }

    @PutMapping("/{tournamentId}/matches/{matchId}/scorers")
    public ResponseEntity<Void> updateMatchScorers(@PathVariable Long tournamentId, @PathVariable Long matchId,
            @RequestBody @Valid UpdateScorersRequest request) {
        log.info("Updating scorers for match {} in tournament {}", matchId, tournamentId);
        tournamentService.updateMatchScorers(tournamentId, matchId, request);
        return ResponseEntity.ok().build();
    }
}
