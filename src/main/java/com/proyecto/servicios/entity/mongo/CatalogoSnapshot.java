package com.proyecto.servicios.entity.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "catalogo_snapshots")
public class CatalogoSnapshot {

    @Id
    private String id;

    private Instant fechaSincronizacion;
    private String codigoRespuesta;
    private String mensajeRespuesta;
    private Integer totalProductos;
    private List<ProductoCatalogo> productos = new ArrayList<>();
}