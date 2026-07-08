package com.tov.futtor.torneo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StandingsRowDto {
    private final Long teamId;
    private final String teamName;
    private final int points;
    private final int goalsFor;
    private final int goalsAgainst;
    private final int goalDifference;
    private final int matchesPlayed;
    private final int wins;
    private final int draws;
    private final int losses;
}
