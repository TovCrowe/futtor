package com.tov.futtor.torneo.mapper;

import java.util.List;

import com.tov.futtor.torneo.dto.MatchDTO;
import com.tov.futtor.torneo.dto.PlayerDto;
import com.tov.futtor.torneo.dto.TeamDto;
import com.tov.futtor.torneo.dto.TournamentDto;
import com.tov.futtor.torneo.entity.Match;
import com.tov.futtor.torneo.entity.Player;
import com.tov.futtor.torneo.entity.Team;
import com.tov.futtor.torneo.entity.Tournament;

public final class TournamentMapper {

    private TournamentMapper() {
    }

    public static MatchDTO toMatchDto(Match match) {
        return new MatchDTO(
                match.getId(),
                match.getHomeTeam().getId(),
                match.getAwayTeam().getId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeGoals(),
                match.getAwayGoals());
    }

    public static List<MatchDTO> toMatchDtos(List<Match> matches) {
        if (matches == null) {
            return List.of();
        }
        return matches.stream().map(TournamentMapper::toMatchDto).toList();
    }

    public static TeamDto toTeamDto(Team team) {
        return new TeamDto(team.getId(), team.getName(), null);
    }

    public static PlayerDto toPlayerDto(Player player) {
        return new PlayerDto(
                player.getId(),
                player.getName(),
                player.getGoals(),
                toTeamDto(player.getTeam()));
    }

    public static TournamentDto toTournamentDto(Tournament tournament) {
        return new TournamentDto(tournament.getId(), tournament.getName(), tournament.isGenerated());
    }
}
