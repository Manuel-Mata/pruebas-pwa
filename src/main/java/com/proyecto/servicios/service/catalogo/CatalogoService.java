package com.proyecto.servicios.service.catalogo;

import com.proyecto.servicios.model.catalogo.CatalogoSincronizacionResponse;
import com.proyecto.servicios.model.catalogo.ProductoCatalogoResponse;

import java.util.List;

public interface CatalogoService {

    /**
     * Consulta el catálogo vigente en GestoPago y guarda un nuevo snapshot en Mongo.
     * No reemplaza snapshots anteriores: cada sincronización agrega un documento nuevo
     * (historial), para poder auditar cómo cambió el catálogo a lo largo del tiempo.
     */
    CatalogoSincronizacionResponse sincronizar();

    /**
     * Devuelve los productos del snapshot más reciente guardado en Mongo.
     * Nunca llama a GestoPago directamente: esa es la razón de ser del snapshot.
     *
     * @param idServicio filtro opcional; si es null, regresa todos los productos del snapshot.
     */
    List<ProductoCatalogoResponse> obtenerProductos(Integer idServicio);
}
