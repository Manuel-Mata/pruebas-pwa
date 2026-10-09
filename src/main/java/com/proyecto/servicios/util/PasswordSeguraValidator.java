package com.proyecto.servicios.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class PasswordSeguraValidator implements ConstraintValidator<PasswordSegura, String> {
    
    private static final Pattern PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$");

    public static boolean esValida(String value) {
        if (value == null) {
            return false;
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        return PATTERN.matcher(value).matches();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return esValida(value);
    }
}
