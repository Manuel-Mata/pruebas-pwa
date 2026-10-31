package com.proyecto.servicios.model.catalogo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductoCatalogoResponse {

    private String servicio;
    private String producto;
    private Integer idServicio;
    private Integer idProducto;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private BigDecimal precio;
    private String tipoReferencia;
    private String legend;
}
