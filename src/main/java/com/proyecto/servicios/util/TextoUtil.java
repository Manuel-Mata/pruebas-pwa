package com.proyecto.servicios.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TextoUtil {
    private TextoUtil() {}

    public static String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String nombreCompleto(String... partes) {
        return Arrays.stream(partes)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(" "));
    }
}
