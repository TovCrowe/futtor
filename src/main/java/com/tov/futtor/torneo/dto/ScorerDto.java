package com.tov.futtor.torneo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Una línea de goleo dentro de un partido: qué jugador, de qué equipo, cuántos goles.
 */
@Getter
@AllArgsConstructor
public class ScorerDto {
    private Long playerId;
    private String playerName;
    private Long teamId;
    private int goals;
}
