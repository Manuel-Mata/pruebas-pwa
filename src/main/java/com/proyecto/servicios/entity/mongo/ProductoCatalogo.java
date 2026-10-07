package com.proyecto.servicios.entity.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductoCatalogo {

    private String servicio;
    private String producto;
    private Integer idServicio;
    private Integer idProducto;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private Boolean hasDigitoVerificador;

    /** Se guarda como número decimal exacto (Decimal128), no como texto. */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal precio;

    private Boolean showAyuda;
    private String tipoReferencia;
    private String legend;
}