package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class EncriptacionGestoPagoException extends TransaccionException {
    public EncriptacionGestoPagoException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
