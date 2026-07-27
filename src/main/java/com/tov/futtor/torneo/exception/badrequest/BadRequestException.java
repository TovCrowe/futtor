package com.tov.futtor.torneo.exception.badrequest;


public abstract class BadRequestException extends RuntimeException {

    protected BadRequestException(String message) {
        super(message);
    }
}
