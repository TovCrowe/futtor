package com.tov.futtor.torneo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateMatchRequest {
    @NotNull
    private Long homeTeamId;
    @NotNull
    private Long awayTeamId;
    @PositiveOrZero
    private int homeTeamGoals;
    @PositiveOrZero
    private int awayTeamGoals;
}
