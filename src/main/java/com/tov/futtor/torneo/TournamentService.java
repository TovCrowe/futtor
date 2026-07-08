package com.tov.futtor.torneo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tov.futtor.torneo.dto.CreateMatchRequest;
import com.tov.futtor.torneo.dto.StandingsRowDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public Tournament createTournament(String name) {
        Tournament tournament = new Tournament(name);
        return tournamentRepository.save(tournament);
    }

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
                        .reversed())
                .toList();
    }

    public void createMatch(Long tournamentId, CreateMatchRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        Team homeTeam = teamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Home team not found"));
        Team awayTeam = teamRepository.findById(request.getAwayTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Away team not found"));

        Match match = new Match(tournament, homeTeam, awayTeam, request.getHomeTeamGoals(), request.getAwayTeamGoals());
        matchRepository.save(match);
    }
}
