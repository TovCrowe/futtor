package com.tov.futtor.torneo.exception;


public abstract class BadRequestException extends RuntimeException {

    protected BadRequestException(String message) {
        super(message);
    }
}
