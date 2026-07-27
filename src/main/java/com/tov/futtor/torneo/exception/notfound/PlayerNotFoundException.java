package com.tov.futtor.torneo.exception.notfound;

public class PlayerNotFoundException extends NotFoundException {

    public PlayerNotFoundException(Long playerId) {
        super("Player not found with ID: " + playerId);
    }
}
