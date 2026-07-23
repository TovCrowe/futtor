package com.tov.futtor.torneo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchDTO {
    private Long id;
    private String homeTeamName;
    private String awayTeamName;
    private Integer homeTeamGoals;
    private Integer awayTeamGoals;
}
