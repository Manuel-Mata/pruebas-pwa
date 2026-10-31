package com.proyecto.servicios.model.externo;

import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CodigoPostalResponse {
    private String cp;
    private String estado;
    private String municipio;
    private List<String> colonias;
}
