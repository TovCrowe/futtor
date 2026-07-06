package com.tov.futtor.torneo;

public class Accumulator {
    final Long teamId;
    final String teamName;
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
