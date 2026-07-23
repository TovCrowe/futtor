package com.tov.futtor.torneo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TournamentDto {
    private final Long id;
    private final String name;
    private final boolean isGenerated;
}
