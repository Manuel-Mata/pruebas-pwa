package com.proyecto.servicios.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordSeguraValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordSegura {
    String message() default "La contraseña debe tener entre 8 y 72 caracteres, con mayúscula, minúscula, número y carácter especial";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
