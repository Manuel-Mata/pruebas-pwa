package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaInactivaException extends TransaccionException {
    public CuentaInactivaException() {
        super("La cuenta no está activa, no se pueden procesar transacciones sobre ella.", HttpStatus.FORBIDDEN);
    }
}
