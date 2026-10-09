package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.model.CuentaCreacionRequest;
import com.proyecto.servicios.model.CuentaEstatusRequest;
import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.model.SaldoResponse;
import com.proyecto.servicios.service.clientes.CuentaService;
import com.proyecto.servicios.util.Patrones;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping("/{numeroCuenta}")
    public CuentaResponse obtener(@PathVariable
            @Pattern(regexp = Patrones.NUMERO_CUENTA, message = "El número de cuenta debe tener 10 dígitos")
            String numeroCuenta) {
        return cuentaService.obtener(numeroCuenta);
    }

    @GetMapping("/{numeroCuenta}/saldo")
    public SaldoResponse saldo(@PathVariable
            @Pattern(regexp = Patrones.NUMERO_CUENTA, message = "El número de cuenta debe tener 10 dígitos")
            String numeroCuenta) {
        return cuentaService.consultarSaldo(numeroCuenta);
    }

    @GetMapping
    public List<CuentaResponse> buscar(
            @RequestParam(required = false) @Positive(message = "El clienteId debe ser mayor a cero") Long clienteId,
            @RequestParam(required = false) EstatusCuenta estatus) {
        return cuentaService.buscar(clienteId, estatus);
    }

    @PostMapping
    public ResponseEntity<CuentaResponse> crear(@Valid @RequestBody CuentaCreacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.crear(request));
    }

    @PatchMapping("/{numeroCuenta}")
    public CuentaResponse actualizarEstatus(@PathVariable
            @Pattern(regexp = Patrones.NUMERO_CUENTA, message = "El número de cuenta debe tener 10 dígitos")
            String numeroCuenta, @Valid @RequestBody CuentaEstatusRequest request) {
        return cuentaService.actualizarEstatus(numeroCuenta, request.getEstatus());
    }
}
