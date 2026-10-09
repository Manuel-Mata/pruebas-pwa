package com.proyecto.servicios.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EstatusCuentaConverter implements AttributeConverter<EstatusCuenta, Boolean> {
    @Override
    public Boolean convertToDatabaseColumn(EstatusCuenta estatus) {
        return estatus == null ? null : estatus == EstatusCuenta.ACTIVA;
    }

    @Override
    public EstatusCuenta convertToEntityAttribute(Boolean activa) {
        if (activa == null) return null;
        return activa ? EstatusCuenta.ACTIVA : EstatusCuenta.INACTIVA;
    }
}
