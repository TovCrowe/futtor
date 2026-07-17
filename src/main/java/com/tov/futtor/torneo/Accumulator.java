package com.tov.futtor.torneo;

import lombok.Getter;


/**
 * this class is used to accumulate the results of a team in a tournament.
 * It keeps track of the team's points, goals for, goals against, goal difference, 
 * matches played, wins, draws, and losses.
 * It provides a method to add the result of a match to the accumulator, updating the relevant
 * Accumulator
 */
@Getter
public class Accumulator {
    private final Long teamId;
    private final String teamName;
    private int points, goalsFor, goalsAgainst, goalDifference, matchesPlayed, wins, draws, losses;

    public Accumulator(Long teamId, String teamName) {
        this.teamId = teamId;
        this.teamName = teamName;
    }

    public void addMatchResult(int goalsFor, int goalsAgainst) {
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        this.goalDifference = this.goalsFor - this.goalsAgainst;
        this.matchesPlayed++;
        if (goalsFor > goalsAgainst) {
            this.wins++;
            this.points += 3;
        } else if (goalsFor == goalsAgainst) {
            this.draws++;
            this.points += 1;
        } else {
            this.losses++;
        }
    }
}
