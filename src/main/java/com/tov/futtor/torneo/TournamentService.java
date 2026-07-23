package com.tov.futtor.torneo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tov.futtor.torneo.dto.CreateTeamRequest;
import com.tov.futtor.torneo.dto.CreateTournamentRequest;
import com.tov.futtor.torneo.dto.MatchDTO;
import com.tov.futtor.torneo.dto.StandingsRowDto;
import com.tov.futtor.torneo.dto.UpdateMatchRequest;
import com.tov.futtor.torneo.entity.Match;
import com.tov.futtor.torneo.entity.Team;
import com.tov.futtor.torneo.entity.Tournament;
import com.tov.futtor.torneo.exception.MatchInvalidException;
import com.tov.futtor.torneo.exception.TournamentNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public Tournament createTournament(CreateTournamentRequest request) {
        Tournament tournament = new Tournament(request.getName());
        return tournamentRepository.save(tournament);
    }

    /**
     * Calculates the standings for a given tournament.
     * It retrieves all teams and matches for the tournament, accumulates the results for each team
     * @param tournamentId
     * @return
     */

    public List<StandingsRowDto> calculateStandings(Long tournamentId) {
        List<Team> teams = teamRepository.findByTournamentId(tournamentId);
        List<Match> matches = matchRepository.findByTournamentId(tournamentId);

        Map<Long, Accumulator> accumulators = new HashMap<>();
        for (Team team : teams) {
            accumulators.put(team.getId(), new Accumulator(team.getId(), team.getName()));
        }

        for (Match match : matches) {
            Accumulator homeTeam = accumulators.get(match.getHomeTeam().getId());
            Accumulator awayTeam = accumulators.get(match.getAwayTeam().getId());

            if (homeTeam == null || awayTeam == null) {
                throw new IllegalStateException("Match contains a team that is not part of the tournament");
            }

            if (!match.getIsPlayed()) {
                continue;
            }

            homeTeam.addMatchResult(match.getHomeGoals(), match.getAwayGoals());
            awayTeam.addMatchResult(match.getAwayGoals(), match.getHomeGoals());
        }

        return accumulators.values().stream()
                .map(a -> new StandingsRowDto(a.getTeamId(), a.getTeamName(), a.getPoints(),
                        a.getGoalsFor(), a.getGoalsAgainst(), a.getGoalDifference(),
                        a.getMatchesPlayed(), a.getWins(), a.getDraws(), a.getLosses()))
                .sorted(Comparator
                        .comparingInt((StandingsRowDto row) -> row.getPoints())
                        .thenComparingInt((StandingsRowDto row) -> row.getGoalDifference())
                        .thenComparingInt((StandingsRowDto row) -> row.getGoalsFor())
                        .reversed()
                        .thenComparing((StandingsRowDto row) -> row.getTeamName()))
                .toList();
    }

    public void createTeam(Long tournamentId, CreateTeamRequest teamRequest) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        Team team = new Team(teamRequest.getName(), tournament);
        teamRepository.save(team);
    }

    @Transactional
    public void createMatchGamesAutomatically(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (tournament.isGenerated()) {
            throw new MatchInvalidException("Matches have already been generated for this tournament");
        }

        List<Team> teams = teamRepository.findByTournamentId(tournamentId);
        if (teams.size() < 2) {
            throw new MatchInvalidException("Not enough teams to create matches");
        }

        List<Match> existingMatches = matchRepository.findByTournamentId(tournamentId);
        for (int i = 0; i < teams.size(); i++) {
            for (int j = 0; j < teams.size(); j++) {
                if (j == i) {
                    continue;
                }
                Match match = new Match(tournament, teams.get(i), teams.get(j), null, null);
                existingMatches.add(match);
            }
        }

        tournament.setGenerated(true);
        tournamentRepository.save(tournament);
        matchRepository.saveAll(existingMatches);
    }

    public void updateMatch(Long tournamentId, Long matchId, UpdateMatchRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchInvalidException("Match not found"));

        if (!match.getTournament().getId().equals(tournament.getId())) {
            throw new MatchInvalidException("Match does not belong to the specified tournament");
        }

        match.setHomeGoals(request.getHomeTeamGoals());
        match.setAwayGoals(request.getAwayTeamGoals());
        matchRepository.save(match);
    }

    public List<MatchDTO> getMatches(Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        List<Match> matches = matchRepository.findByTournamentId(tournamentId);

        return matches.stream()
                .map(match -> new MatchDTO(
                        match.getId(),
                        match.getHomeTeam().getName(),
                        match.getAwayTeam().getName(),
                        match.getHomeGoals(),
                        match.getAwayGoals()))
                .toList();
    }
}
