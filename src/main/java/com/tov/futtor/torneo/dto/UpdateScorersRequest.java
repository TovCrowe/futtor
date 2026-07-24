package com.tov.futtor.torneo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Captura del goleo de un partido: la lista completa de jugadores con sus goles.
 * Reemplaza lo que hubiera antes, por eso puede venir con goles en cero.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScorersRequest {

    @NotNull
    @Valid
    private List<ScorerEntry> scorers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScorerEntry {

        @NotNull
        private Long playerId;

        @NotNull
        @PositiveOrZero
        private Integer goals;
    }
}
