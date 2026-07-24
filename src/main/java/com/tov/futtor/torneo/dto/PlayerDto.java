package com.tov.futtor.torneo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerDto {
    private Long id;
    private String name;
    private int goals;
    private TeamDto team;
}
