package com.tov.futtor.torneo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdatePlayerRequest {
    private String name;
    private int goals;
}
