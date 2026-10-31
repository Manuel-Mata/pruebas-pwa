package com.proyecto.servicios.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {

    private static final Pattern PATRON =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$");

    public static boolean esValida(String valor) {
        return valor != null
                && valor.getBytes(StandardCharsets.UTF_8).length <= 72
                && PATRON.matcher(valor).matches();
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        return valor == null || esValida(valor);   // el nulo lo avisa @NotBlank, sin duplicar mensajes
    }
}
