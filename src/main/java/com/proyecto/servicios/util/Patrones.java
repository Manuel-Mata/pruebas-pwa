package com.proyecto.servicios.util;

public final class Patrones {
    private Patrones() {}

    public static final String NOMBRE =
            "^[A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+( [A-Za-zÁÉÍÓÚÜáéíóúüÑñ]+)*$";
    public static final String CURP =
            "^[A-Z]{4}\\d{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d$";
    public static final String RFC =
            "^[A-ZÑ&]{3,4}\\d{6}[A-Z\\d]{3}$";
    public static final String TEXTO_LIBRE =
            "^[\\p{L}\\p{N}][\\p{L}\\p{N} .,#°'/&()-]*$";
    public static final String TELEFONO = "^\\d{10}$";
    public static final String CP = "^\\d{5}$";
}
