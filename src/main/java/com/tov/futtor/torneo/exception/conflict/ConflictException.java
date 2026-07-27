package com.tov.futtor.torneo.exception.conflict;


public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
