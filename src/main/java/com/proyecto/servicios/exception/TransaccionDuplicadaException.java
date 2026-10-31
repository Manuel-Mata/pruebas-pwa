package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class TransaccionDuplicadaException extends TransaccionException {
    public TransaccionDuplicadaException(String upc) {
        super("Ya existe una transacción con el UPC '" + upc + "'.", HttpStatus.CONFLICT);
    }
}
