package com.tov.futtor.torneo.exception.conflict;

public class PlayerHasScorerRecordsException extends ConflictException {

    public PlayerHasScorerRecordsException(Long playerId, long goals) {
        super("Cannot delete player with ID: " + playerId + " because they have " + goals
                + " goal(s) registered");
    }
}
