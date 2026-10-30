package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class LimiteDiarioSincronizacionException extends CatalogoException {

    public LimiteDiarioSincronizacionException(int maximo) {
        super("Se alcanzó el máximo de " + maximo
                + " sincronización(es) del catálogo permitidas hoy", HttpStatus.TOO_MANY_REQUESTS);
    }
}