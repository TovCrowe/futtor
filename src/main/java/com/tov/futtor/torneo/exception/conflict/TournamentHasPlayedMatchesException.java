package com.tov.futtor.torneo.exception.conflict;

public class TournamentHasPlayedMatchesException extends ConflictException {

    public TournamentHasPlayedMatchesException(Long tournamentId) {
        super("Cannot delete tournament with ID: " + tournamentId
                + " because it has played matches. Use force=true to delete it anyway");
    }
}
