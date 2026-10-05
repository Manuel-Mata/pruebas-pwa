package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class RespuestaNoExitosaGestoPagoException extends CatalogoException {

    public RespuestaNoExitosaGestoPagoException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, cause);
    }
}