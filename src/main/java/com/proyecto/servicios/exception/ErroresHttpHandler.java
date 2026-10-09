package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class ErroresHttpHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        log.warn("Método HTTP no permitido");
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para este recurso.");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<GenericResponse> noEncontrado(Exception ex) {
        log.warn("Recurso no encontrado");
        return responder(HttpStatus.NOT_FOUND, "El recurso solicitado no existe.");
    }

    private ResponseEntity<GenericResponse> responder(HttpStatus status, String mensaje) {
        GenericResponse r = new GenericResponse();
        r.setCodigo(status.value());
        r.setMensaje(mensaje);
        return ResponseEntity.status(status).body(r);
    }
}
