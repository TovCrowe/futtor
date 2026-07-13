package com.tov.futtor.torneo.exception;


public class TournamentNotFoundException extends NotFoundException {

    public TournamentNotFoundException(Long tournamentId) {
        super("Tournament not found with ID: " + tournamentId);
    }
}
