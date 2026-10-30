package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class AutenticacionGestoPagoException extends CatalogoException {

    public AutenticacionGestoPagoException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public AutenticacionGestoPagoException(String message, Throwable cause) {
        super(message, HttpStatus.UNAUTHORIZED, cause);
    }
}