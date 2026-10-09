package com.proyecto.servicios.model.externo;

import lombok.Data;
import java.util.List;

@Data
public class PostaliCpResponse {
    private String cp;
    private String estado;
    private String municipio;
    private List<AsentamientoResponse> asentamientos;

    @Data
    public static class AsentamientoResponse {
        private String nombre;
        private String zona;
        private String asenta_slug;
    }
}
