package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * La cuenta sobre la que se solicita la operación no pertenece al cliente
 * autenticado (identificado por el JWT de la petición).
 */
public class OperacionNoAutorizadaException extends TransaccionException {
    public OperacionNoAutorizadaException() {
        super("No tiene autorización para operar sobre esta cuenta.", HttpStatus.FORBIDDEN);
    }
}
