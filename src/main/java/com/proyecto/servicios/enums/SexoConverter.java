package com.proyecto.servicios.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SexoConverter implements AttributeConverter<Sexo, String> {

    @Override
    public String convertToDatabaseColumn(Sexo sexo) {
        if (sexo == null) {
            return null;
        }
        return sexo.getCodigo();
    }

    @Override
    public Sexo convertToEntityAttribute(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Sexo s : Sexo.values()) {
            if (s.getCodigo().equals(codigo)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Código de sexo desconocido: " + codigo);
    }
}
