package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class GenericResponse<T> {
    private Integer codigo;
    private String mensaje;
    private T datos;

    public static <T> GenericResponse<T> exito(int codigo, String mensaje, T datos) {
        return new GenericResponse<>(codigo, mensaje, datos);
    }

    public static <T> GenericResponse<T> exito(String mensaje, T datos) {
        return new GenericResponse<>(200, mensaje, datos);
    }

    public static <T> GenericResponse<T> exito(T datos) {
        return new GenericResponse<>(200, "Operación exitosa.", datos);
    }

    public static GenericResponse<Void> exito(String mensaje) {
        return new GenericResponse<>(200, mensaje, null);
    }

    public static <T> GenericResponse<T> error(int codigo, String mensaje) {
        return new GenericResponse<>(codigo, mensaje, null);
    }

    public static <T> GenericResponse<T> error(int codigo, String mensaje, T datos) {
        return new GenericResponse<>(codigo, mensaje, datos);
    }
}
