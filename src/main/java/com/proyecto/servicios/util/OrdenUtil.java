package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.onboarding.ErrorValidacionException;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.TreeSet;

public final class OrdenUtil {
    private OrdenUtil() {}

    public static void validar(Pageable pageable, Set<String> permitidos) {
        pageable.getSort().forEach(orden -> {
            if (!permitidos.contains(orden.getProperty())) {
                throw new ErrorValidacionException("Campo de ordenamiento no permitido. Use: "
                        + String.join(", ", new TreeSet<>(permitidos)) + ".");
            }
        });
    }
}
