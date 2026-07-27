package com.tov.futtor.torneo.exception.notfound;


public class TournamentNotFoundException extends NotFoundException {

    public TournamentNotFoundException(Long tournamentId) {
        super("Tournament not found with ID: " + tournamentId);
    }
}
