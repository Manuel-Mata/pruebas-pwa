package com.proyecto.servicios.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextoUtil {
    private TextoUtil() {}

    public static String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
