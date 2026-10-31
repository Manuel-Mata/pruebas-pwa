package com.proyecto.servicios.controller.catalogo;

import com.proyecto.servicios.model.catalogo.CatalogoSincronizacionResponse;
import com.proyecto.servicios.model.catalogo.ProductoCatalogoResponse;
import com.proyecto.servicios.service.catalogo.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalogos")
@RequiredArgsConstructor
public class CatalogoGestoPagoController {

    private final CatalogoService catalogoService;

    /**
     * Consulta el catálogo vigente en GestoPago y lo guarda como un nuevo snapshot en Mongo.
     * Limitado a {@code gestopago.catalogo.max-sincronizaciones-diarias} veces al día:
     * GestoPago bloquea la IP si se llama a getProductList.do más de 3 veces en un día.
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<CatalogoSincronizacionResponse> sincronizar() {
        return ResponseEntity.ok(catalogoService.sincronizar());
    }

    /**
     * Consulta el catálogo ya guardado en Mongo (el último snapshot sincronizado).
     * Nunca golpea la API de GestoPago directamente.
     */
    @GetMapping("/productos")
    public ResponseEntity<List<ProductoCatalogoResponse>> obtenerProductos(
            @RequestParam(required = false) Integer idServicio) {
        return ResponseEntity.ok(catalogoService.obtenerProductos(idServicio));
    }
}
