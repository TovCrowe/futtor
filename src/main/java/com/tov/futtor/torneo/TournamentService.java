package com.tov.futtor.torneo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tov.futtor.torneo.dto.StandingsRowDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public List<StandingsRowDto> calculateStandings(Long tournamentId) {
        List<Team> teams = teamRepository.findByTournamentId(tournamentId);
        List<Match> matches = matchRepository.findByTournamentId(tournamentId);

        HashMap<Long, StandingsRowDto> standingsMap = new HashMap<>();
        for (Team team : teams) {
            standingsMap.put(team.getId(), new StandingsRowDto(team.getId(), team.getName(), 0, 0, 0, 0, 0, 0, 0, 0));
        }

        for (Match match : matches) {
            StandingsRowDto homeTeamStanding = standingsMap.get(match.getHomeTeam().getId());
            StandingsRowDto awayTeamStanding = standingsMap.get(match.getAwayTeam().getId());

            homeTeamStanding.setMatchesPlayed(homeTeamStanding.getMatchesPlayed() + 1);
            awayTeamStanding.setMatchesPlayed(awayTeamStanding.getMatchesPlayed() + 1);

            homeTeamStanding.setGoalsFor(homeTeamStanding.getGoalsFor() + match.getHomeGoals());
            homeTeamStanding.setGoalsAgainst(homeTeamStanding.getGoalsAgainst() + match.getAwayGoals());
            awayTeamStanding.setGoalsFor(awayTeamStanding.getGoalsFor() + match.getAwayGoals());
            awayTeamStanding.setGoalsAgainst(awayTeamStanding.getGoalsAgainst() + match.getHomeGoals());

            if (match.getHomeGoals() > match.getAwayGoals()) {
                homeTeamStanding.setWins(homeTeamStanding.getWins() + 1);
                homeTeamStanding.setPoints(homeTeamStanding.getPoints() + 3);
                awayTeamStanding.setLosses(awayTeamStanding.getLosses() + 1);
            } else if (match.getHomeGoals() < match.getAwayGoals()) {
                awayTeamStanding.setWins(awayTeamStanding.getWins() + 1);
                awayTeamStanding.setPoints(awayTeamStanding.getPoints() + 3);
                homeTeamStanding.setLosses(homeTeamStanding.getLosses() + 1);
            } else {
                homeTeamStanding.setDraws(homeTeamStanding.getDraws() + 1);
                awayTeamStanding.setDraws(awayTeamStanding.getDraws() + 1);
                homeTeamStanding.setPoints(homeTeamStanding.getPoints() + 1);
                awayTeamStanding.setPoints(awayTeamStanding.getPoints() + 1);
            }
        }

        for (StandingsRowDto standing : standingsMap.values()) {
            standing.setGoalDifference(standing.getGoalsFor() - standing.getGoalsAgainst());
        }

        return standingsMap.values().stream().sorted(Comparator
                .comparingInt(StandingsRowDto::getPoints)
                .thenComparingInt(StandingsRowDto::getGoalDifference)
                .thenComparingInt(StandingsRowDto::getGoalsFor)
                .reversed())
        .toList();
    }
}
