package com.proyecto.servicios.enums;

public enum Sexo {
    HOMBRE("H"),
    MUJER("M");

    private final String codigo;

    Sexo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
