package com.proyecto.servicios.controller.transacciones;

import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.transaccion.TransaccionSolicitudRequest;
import com.proyecto.servicios.service.transacciones.TransaccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;

    /**
     * Procesa un pago PostPago contra GestoPago (sendTx.do) sobre una cuenta.
     * Requiere un idempotencyKey único por intento, provisto por el cliente.
     */
    @PostMapping
    public ResponseEntity<TransaccionResponse> procesar(@Valid @RequestBody TransaccionSolicitudRequest request) {
        return ResponseEntity.ok(transaccionService.procesar(request));
    }
}
