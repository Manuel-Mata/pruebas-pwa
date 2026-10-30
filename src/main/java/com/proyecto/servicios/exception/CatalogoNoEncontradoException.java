package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CatalogoNoEncontradoException extends CatalogoException {

    public CatalogoNoEncontradoException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}