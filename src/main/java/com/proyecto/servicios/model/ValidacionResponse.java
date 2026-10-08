package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ValidacionResponse extends GenericResponse {
    private Map<String, String> errores;
}
