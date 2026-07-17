package com.tov.futtor.torneo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tov.futtor.torneo.dto.CreateMatchRequest;
import com.tov.futtor.torneo.dto.CreateTeamRequest;
import com.tov.futtor.torneo.dto.StandingsRowDto;
import com.tov.futtor.torneo.entity.Match;
import com.tov.futtor.torneo.entity.Team;
import com.tov.futtor.torneo.entity.Tournament;
import com.tov.futtor.torneo.exception.MatchInvalidException;
import com.tov.futtor.torneo.exception.TeamNotFoundException;
import com.tov.futtor.torneo.exception.TournamentNotFoundException;

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
                        .thenComparing((StandingsRowDto row) -> row.getTeamName())
)
                .toList();
    }

    public void createMatch(Long tournamentId, CreateMatchRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        Team homeTeam = teamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new TeamNotFoundException(request.getHomeTeamId()));
        Team awayTeam = teamRepository.findById(request.getAwayTeamId())
                .orElseThrow(() -> new TeamNotFoundException(request.getAwayTeamId()));

        if (!homeTeam.getTournament().getId().equals(tournamentId) || !awayTeam.getTournament().getId().equals(tournamentId)) {
            throw new MatchInvalidException("Both teams must belong to the specified tournament");
        }

        if (homeTeam.getId().equals(awayTeam.getId())) {
            throw new MatchInvalidException("A team cannot play against itself");
        }

        Match match = new Match(tournament, homeTeam, awayTeam, request.getHomeTeamGoals(), request.getAwayTeamGoals());
        matchRepository.save(match);
    }

    public void createTeam(Long tournamentId, CreateTeamRequest teamRequest) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        Team team = new Team(teamRequest.getName(), tournament);
        teamRepository.save(team);
    }
}
