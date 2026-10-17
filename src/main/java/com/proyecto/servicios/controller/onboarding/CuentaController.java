package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.CuentaBusquedaRequest;
import com.proyecto.servicios.model.CuentaCreacionRequest;
import com.proyecto.servicios.model.CuentaEstatusRequest;
import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.clientes.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping
    public ResponseEntity<GenericResponse<CuentaResponse>> crear(@Valid @RequestBody CuentaCreacionRequest request) {
        CuentaResponse response = cuentaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito(HttpStatus.CREATED.value(), "Cuenta creada exitosamente.", response));
    }

    @PostMapping("/buscar")
    public ResponseEntity<GenericResponse<List<CuentaResponse>>> buscar(@Valid @RequestBody(required = false) CuentaBusquedaRequest request) {
        CuentaBusquedaRequest req = (request != null) ? request : new CuentaBusquedaRequest();
        List<CuentaResponse> cuentas = cuentaService.buscar(req);
        return ResponseEntity.ok(GenericResponse.exito("Búsqueda de cuentas realizada exitosamente.", cuentas));
    }

    @PatchMapping
    public ResponseEntity<GenericResponse<CuentaResponse>> actualizarEstatus(@Valid @RequestBody CuentaEstatusRequest request) {
        CuentaResponse response = cuentaService.actualizarEstatus(request);
        return ResponseEntity.ok(GenericResponse.exito("Estatus de la cuenta actualizado exitosamente.", response));
    }
}
