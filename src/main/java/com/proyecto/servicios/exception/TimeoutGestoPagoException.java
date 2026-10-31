package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class TimeoutGestoPagoException extends CatalogoException {

    public TimeoutGestoPagoException(String message, Throwable cause) {
        super(message, HttpStatus.GATEWAY_TIMEOUT, cause);
    }
}