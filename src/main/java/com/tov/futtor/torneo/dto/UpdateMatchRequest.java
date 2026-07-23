package com.tov.futtor.torneo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateMatchRequest {
    @NotNull
    @PositiveOrZero
    private Integer homeTeamGoals;
    @NotNull
    @PositiveOrZero
    private Integer awayTeamGoals;
}
