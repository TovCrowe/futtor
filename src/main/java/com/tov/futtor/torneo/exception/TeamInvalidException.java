package com.tov.futtor.torneo.exception;

public class TeamInvalidException extends BadRequestException {

    public TeamInvalidException(String message) {
        super(message);
    }
}
