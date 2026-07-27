package com.tov.futtor.torneo.exception.notfound;

public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
