package com.tov.futtor.torneo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @NotNull
    private Team homeTeam;

    @ManyToOne
    @NotNull
    private Team awayTeam;

    @NotNull
    @PositiveOrZero
    private Integer homeGoals;

    @NotNull
    @ManyToOne
    private Tournament tournament;

    @NotNull
    @PositiveOrZero
    private Integer awayGoals;

    public Match(Tournament tournament, Team homeTeam, Team awayTeam, Integer homeGoals, Integer awayGoals) {
        this.tournament = tournament;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }
}
