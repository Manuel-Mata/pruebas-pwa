package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogoException.class)
    public ResponseEntity<GenericResponse> manejarCatalogoException(CatalogoException ex) {
        log.error("Error en integración de catálogo: {} - {}",
                ex.getClass().getSimpleName(), ex.getMessage());

        GenericResponse response = new GenericResponse();
        response.setCodigo(ex.getStatus().value());
        response.setMensaje(ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
}