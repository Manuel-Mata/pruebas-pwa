package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class SaldoInsuficienteException extends TransaccionException {
    public SaldoInsuficienteException() {
        super("La cuenta no tiene saldo suficiente para realizar esta transacción.", HttpStatus.PAYMENT_REQUIRED);
    }
}
