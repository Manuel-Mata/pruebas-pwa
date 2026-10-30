package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;
import com.proyecto.servicios.exception.onboarding.CurpDuplicadaException;
import com.proyecto.servicios.exception.onboarding.OnboardingException;
import com.proyecto.servicios.exception.onboarding.RfcDuplicadoException;
import com.proyecto.servicios.model.GenericResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, Supplier<OnboardingException>> DUPLICADOS = Map.of(
            "uq_clientes_curp", CurpDuplicadaException::new,
            "uq_clientes_rfc", RfcDuplicadoException::new,
            "uq_clientes_correo", CorreoDuplicadoException::new,
            "uq_usuarios_correo", CorreoDuplicadoException::new
    );

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<GenericResponse<Void>> handleOnboardingException(OnboardingException ex) {
        log.warn("Excepción de negocio: {} - Status: {}", ex.getMessage(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .body(GenericResponse.error(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Map<String, String>>> handleValidacionesDto(MethodArgumentNotValidException ex) {
        log.warn("Error de validación en DTO");
        Map<String, String> errores = new TreeMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) {
            errores.merge(e.getField(), e.getDefaultMessage(), (a, b) -> a + "; " + b);
        }
        String mensaje = "Datos no válidos: " + errores;
        GenericResponse<Map<String, String>> response = GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje, errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Violación de restricción: {}", ex.getMessage());
        String detalle = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(" | "));
        String mensaje = detalle.isBlank() ? "Los parámetros enviados no cumplen las validaciones requeridas." : detalle;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GenericResponse<Void>> handleValidacionMetodo(HandlerMethodValidationException ex) {
        log.warn("HandlerMethodValidationException capturada");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), "Los datos de la petición no cumplen con las reglas requeridas."));
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<GenericResponse<Void>> handleOrdenInvalido(PropertyReferenceException ex) {
        log.warn("PropertyReferenceException: campo de orden no válido: {}", ex.getPropertyName());
        String mensaje = "El campo de ordenamiento '" + ex.getPropertyName() + "' no es válido.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof UnrecognizedPropertyException upe) {
            String campo = upe.getPropertyName();
            log.warn("Propiedad no permitida o desconocida en JSON: {}", campo);
            String mensaje = "El campo '" + campo + "' no está permitido en esta petición o es controlado por el servidor.";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
        }

        if (cause instanceof MismatchedInputException mie) {
            String campo = mie.getPath().stream()
                    .map(r -> r.getFieldName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            Class<?> targetType = mie.getTargetType();
            String detalle = "El campo '" + (campo.isBlank() ? "solicitud" : campo) + "' tiene un tipo de dato incorrecto.";
            if (targetType != null && targetType.isEnum()) {
                detalle += " Valores permitidos: " + Arrays.toString(targetType.getEnumConstants());
            } else if (campo.toLowerCase(Locale.ROOT).contains("fecha")) {
                detalle += " Formato esperado: yyyy-MM-dd.";
            }
            log.warn("MismatchedInputException: {}", detalle);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), detalle));
        }

        log.warn("Cuerpo JSON ilegible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), "El cuerpo de la petición no tiene un formato JSON válido."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        String tipoEsperado = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido";
        String mensaje = "El valor '" + ex.getValue() + "' no es válido para el parámetro '" + ex.getName() + "'. Se esperaba tipo: " + tipoEsperado + ".";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String restriccion = extraerRestriccion(ex);
        log.warn("Violación de integridad en base de datos. Restricción={}", restriccion);

        if (restriccion != null) {
            Supplier<OnboardingException> duplicado = DUPLICADOS.get(restriccion);
            if (duplicado != null) {
                OnboardingException oe = duplicado.get();
                return ResponseEntity.status(oe.getStatus())
                        .body(GenericResponse.error(oe.getStatus().value(), oe.getMessage()));
            }
            if (restriccion.startsWith("ck_")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), "Los datos enviados no cumplen con las reglas de validación del sistema."));
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GenericResponse.error(HttpStatus.CONFLICT.value(), "No fue posible guardar la información por un conflicto con los datos existentes."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Método HTTP no soportado: {}", ex.getMethod());
        String mensaje = "El método HTTP " + ex.getMethod() + " no está permitido para este recurso. Métodos soportados: " + Arrays.toString(ex.getSupportedMethods());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(GenericResponse.error(HttpStatus.METHOD_NOT_ALLOWED.value(), mensaje));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<GenericResponse<Void>> handleNoEncontrado(Exception ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GenericResponse.error(HttpStatus.NOT_FOUND.value(), "El recurso o endpoint solicitado no existe."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type no soportado: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(GenericResponse.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "Tipo de contenido no soportado. Se requiere Content-Type: application/json."));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<GenericResponse<Void>> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.warn("Media type no aceptable");
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(GenericResponse.error(HttpStatus.NOT_ACCEPTABLE.value(), "El formato de respuesta solicitado no es soportado por el servidor."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GenericResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Parámetro requerido ausente: {}", ex.getParameterName());
        String mensaje = "El parámetro obligatorio '" + ex.getParameterName() + "' de tipo " + ex.getParameterType() + " no fue proporcionado.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(HttpStatus.BAD_REQUEST.value(), mensaje));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Void>> handleGenerico(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Error interno del servidor no controlado [ID={}]: {}", errorId, ex.getMessage(), ex);
        String mensaje = "Ha ocurrido un error inesperado en el servidor. Código de seguimiento: " + errorId;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenericResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), mensaje));
    }

    private String extraerRestriccion(DataIntegrityViolationException ex) {
        if (ex.getMostSpecificCause() instanceof PSQLException psql
                && psql.getServerErrorMessage() != null
                && psql.getServerErrorMessage().getConstraint() != null) {
            return psql.getServerErrorMessage().getConstraint().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    @ExceptionHandler(CatalogoException.class)
    public ResponseEntity<GenericResponse<Void>> manejarCatalogoException(CatalogoException ex) {
        log.error("Error en integración de catálogo: {} - {}",
                ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(GenericResponse.error(ex.getStatus().value(), ex.getMessage()));
    }
}
