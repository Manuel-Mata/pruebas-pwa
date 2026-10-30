package com.proyecto.servicios.model.catalogo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoSincronizacionResponse {

    private Instant fechaSincronizacion;
    private String codigoRespuesta;
    private String mensajeRespuesta;
    private Integer totalProductos;
}
