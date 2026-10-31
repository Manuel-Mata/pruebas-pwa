package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base para errores de negocio propios del dominio de transacciones
 * (sendTx), separada de CatalogoException (cliente GestoPago/catálogo) y de
 * OnboardingException (clientes/cuentas), ya que es un contexto distinto.
 */
public abstract class TransaccionException extends RuntimeException {

    private final HttpStatus status;

    protected TransaccionException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
