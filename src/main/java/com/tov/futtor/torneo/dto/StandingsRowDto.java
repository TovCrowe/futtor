package com.tov.futtor.torneo.dto;

import jakarta.annotation.Generated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class StandingsRowDto{
    private Long teamId;
    private String teamName;
    private int points;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
    private int matchesPlayed;
    private int wins;
    private int draws;
    private int losses;
}
