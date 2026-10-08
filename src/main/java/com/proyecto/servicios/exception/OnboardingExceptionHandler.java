package com.proyecto.servicios.exception;

import com.proyecto.servicios.exception.onboarding.OnboardingException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ValidacionResponse;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.proyecto.servicios.exception.onboarding.CurpDuplicadaException;
import com.proyecto.servicios.exception.onboarding.RfcDuplicadoException;
import com.proyecto.servicios.exception.onboarding.CorreoDuplicadoException;

@Slf4j
@RestControllerAdvice
public class OnboardingExceptionHandler {

    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<GenericResponse> handleOnboardingException(OnboardingException ex) {
        log.warn("OnboardingException: {}", ex.getMessage());
        return construirRespuesta(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidacionResponse> handleValidations(MethodArgumentNotValidException ex) {
        log.warn("MethodArgumentNotValidException atrapada.");
        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        ValidacionResponse response = new ValidacionResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje("Errores de validación en la petición.");
        response.setErrores(errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("ConstraintViolationException: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Datos de petición inválidos.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Tipo de dato incorrecto en la petición.");
    }

    private static final Pattern RESTRICCION = Pattern.compile("constraint \"([^\"]+)\"");

    private static final Map<String, Supplier<OnboardingException>> DUPLICADOS = Map.of(
            "uq_clientes_curp",   CurpDuplicadaException::new,
            "uq_clientes_rfc",    RfcDuplicadoException::new,
            "uq_clientes_correo", CorreoDuplicadoException::new,
            "uq_usuarios_correo", CorreoDuplicadoException::new
    );

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String restriccion = extraerRestriccion(ex);
        // Solo el nombre de la restricción: el detalle de la BD trae los valores del cliente
        log.warn("Violación de integridad. restriccion={}", restriccion);

        Supplier<OnboardingException> duplicado = DUPLICADOS.get(restriccion);
        if (duplicado != null) {
            return handleOnboardingException(duplicado.get());
        }
        if (restriccion != null && restriccion.startsWith("ck_")) {
            return construirRespuesta(HttpStatus.BAD_REQUEST,
                    "Los datos enviados no cumplen las reglas de validación.");
        }
        return construirRespuesta(HttpStatus.CONFLICT,
                "No fue posible guardar la información por un conflicto con los datos existentes.");
    }

    private String extraerRestriccion(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve
                && cve.getConstraintName() != null) {
            return cve.getConstraintName().toLowerCase(Locale.ROOT);
        }
        // Plan B: extraer solo el nombre del mensaje, sin loguearlo completo
        String msg = ex.getMostSpecificCause().getMessage();
        Matcher m = (msg == null) ? null : RESTRICCION.matcher(msg);
        return (m != null && m.find()) ? m.group(1).toLowerCase(Locale.ROOT) : null;
    }

    private ResponseEntity<GenericResponse> construirRespuesta(HttpStatus status, String mensaje) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(status.value());
        response.setMensaje(mensaje);
        return ResponseEntity.status(status).body(response);
    }
}
