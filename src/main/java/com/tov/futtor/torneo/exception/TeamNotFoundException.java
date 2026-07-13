package com.tov.futtor.torneo.exception;

public class TeamNotFoundException extends NotFoundException {

    public TeamNotFoundException(Long teamId) {
        super("Team not found with ID: " + teamId);
    }
}
