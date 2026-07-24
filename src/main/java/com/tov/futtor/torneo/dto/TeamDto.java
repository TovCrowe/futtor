package com.tov.futtor.torneo.dto;



import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeamDto {

    private Long id;

    private String name;

    private List<PlayerDto> players;

}
