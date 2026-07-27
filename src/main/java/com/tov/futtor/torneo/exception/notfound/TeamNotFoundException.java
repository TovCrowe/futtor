package com.tov.futtor.torneo.exception.notfound;


public class TeamNotFoundException extends NotFoundException {

    public TeamNotFoundException(Long teamId) {
        super("Team not found with ID: " + teamId);
    }
}
