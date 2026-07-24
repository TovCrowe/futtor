package com.tov.futtor.torneo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Scorer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    private Player player;

    @NotNull
    @ManyToOne
    private Match match;

    @PositiveOrZero
    private int goals;  

    public Scorer(Player player, Match match, int goals) {
        this.player = player;
        this.match = match;
        this.goals = goals;
    }
}
