package com.tov.futtor.torneo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateTournamentRequest {
    
    @NotBlank
    private String name;
}
