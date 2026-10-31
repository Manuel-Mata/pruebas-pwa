package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ComunicacionGestoPagoException extends CatalogoException {

    public ComunicacionGestoPagoException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}