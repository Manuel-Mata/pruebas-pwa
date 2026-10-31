package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public abstract class CatalogoException extends RuntimeException {

    private final HttpStatus status;

    protected CatalogoException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    protected CatalogoException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}