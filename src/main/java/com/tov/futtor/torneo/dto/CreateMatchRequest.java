package com.tov.futtor.torneo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMatchRequest {
    @NotNull
    private Long homeTeamId;
    @NotNull
    private Long awayTeamId;
    @PositiveOrZero
    private int homeTeamGoals;
    @PositiveOrZero
    private int awayTeamGoals;

    public CreateMatchRequest(Long homeTeamId, Long awayTeamId, int homeTeamGoals, int awayTeamGoals) {
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.homeTeamGoals = homeTeamGoals;
        this.awayTeamGoals = awayTeamGoals;
    }
}
