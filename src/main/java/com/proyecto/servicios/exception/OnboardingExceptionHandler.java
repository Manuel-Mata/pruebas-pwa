package com.proyecto.servicios.exception;

import com.proyecto.servicios.exception.onboarding.OnboardingException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ValidacionResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.ErrorResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.Objects;
import org.postgresql.util.PSQLException;
import com.proyecto.servicios.exception.onboarding.CurpDuplicadaException;
import com.proyecto.servicios.exception.onboarding.RfcDuplicadoException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;

@Slf4j
@RestControllerAdvice(basePackages = "com.proyecto.servicios.controller.onboarding")
public class OnboardingExceptionHandler {

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<GenericResponse> handleOnboardingException(OnboardingException ex) {
        log.warn("OnboardingException: {}", ex.getMessage());
        return construirRespuesta(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidacionResponse> handleValidations(MethodArgumentNotValidException ex) {
        log.warn("MethodArgumentNotValidException atrapada.");
        Map<String, String> errores = new TreeMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) {
            errores.merge(e.getField(), e.getDefaultMessage(), (a, b) -> a + "; " + b);
        }
        String mensaje = errores.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(" | "));

        ValidacionResponse response = new ValidacionResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje(mensaje);
        response.setErrores(errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("ConstraintViolationException: {}", ex.getMessage());
        String detalle = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining(" | "));
        return construirRespuesta(HttpStatus.BAD_REQUEST, detalle.isBlank() ? "Datos de petición inválidos." : detalle);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GenericResponse> handleValidacionMetodo(HandlerMethodValidationException ex) {
        log.warn("HandlerMethodValidationException atrapada.");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Parámetros de la petición inválidos.");
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<GenericResponse> handleOrdenInvalido(PropertyReferenceException ex) {
        log.warn("PropertyReferenceException atrapada.");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Campo de ordenamiento inválido.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String campo = "";
        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException mie) {
            campo = mie.getPath().stream().map(r -> r.getFieldName())
                    .filter(Objects::nonNull).collect(Collectors.joining("."));
        }
        log.warn("Cuerpo ilegible. campo={}", campo);
        String mensaje = campo.isBlank()
                ? "Cuerpo de la petición inválido o con formato JSON incorrecto."
                : "El campo '" + campo + "' tiene un valor o formato inválido.";
        
        if (campo.startsWith("fecha")) {
            mensaje += " Formato esperado: yyyy-MM-dd.";
        }
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Tipo de dato incorrecto en la petición.");
    }

    private static final Map<String, Supplier<OnboardingException>> DUPLICADOS = Map.of(
            "uq_clientes_curp",   CurpDuplicadaException::new,
            "uq_clientes_rfc",    RfcDuplicadoException::new,
            "uq_clientes_correo", CorreoDuplicadoException::new,
            "uq_usuarios_correo", CorreoDuplicadoException::new
    );

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String restriccion = extraerRestriccion(ex);
        log.warn("Violación de integridad. restriccion={}", restriccion);

        if (restriccion != null) {
            Supplier<OnboardingException> duplicado = DUPLICADOS.get(restriccion);
            if (duplicado != null) {
                return handleOnboardingException(duplicado.get());
            }
            if (restriccion.startsWith("ck_")) {
                return construirRespuesta(HttpStatus.BAD_REQUEST,
                        "Los datos enviados no cumplen las reglas de validación.");
            }
        }
        return construirRespuesta(HttpStatus.CONFLICT,
                "No fue posible guardar la información por un conflicto con los datos existentes.");
    }

    private String extraerRestriccion(DataIntegrityViolationException ex) {
        if (ex.getMostSpecificCause() instanceof org.postgresql.util.PSQLException psql
                && psql.getServerErrorMessage() != null
                && psql.getServerErrorMessage().getConstraint() != null) {
            return psql.getServerErrorMessage().getConstraint().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            HttpRequestMethodNotSupportedException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<GenericResponse> handleErroresHttp(Exception ex) {
        HttpStatus status = (ex instanceof ErrorResponse er)
                ? HttpStatus.valueOf(er.getStatusCode().value())
                : HttpStatus.BAD_REQUEST;
        log.warn("Error HTTP: {}", ex.getClass().getSimpleName());
        String mensaje = switch (status.value()) {
            case 415 -> "Tipo de contenido no soportado. Use application/json.";
            case 405 -> "Método HTTP no permitido para este recurso.";
            case 406 -> "Formato de respuesta no soportado.";
            default -> "Falta un parámetro obligatorio o es inválido.";
        };
        return construirRespuesta(status, mensaje);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenerico(Exception ex) {
        log.error("Error interno del servidor no controlado", ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado en el servidor.");
    }

    private ResponseEntity<GenericResponse> construirRespuesta(HttpStatus status, String mensaje) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(status.value());
        response.setMensaje(mensaje);
        return ResponseEntity.status(status).body(response);
    }
}
