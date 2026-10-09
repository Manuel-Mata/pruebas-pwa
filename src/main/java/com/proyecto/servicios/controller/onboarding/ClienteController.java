package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.ClienteRegistroRequest;
import com.proyecto.servicios.model.ClienteRegistroResponse;
import com.proyecto.servicios.model.ClienteActualizacionRequest;
import com.proyecto.servicios.model.ClienteDetalleResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ClienteFiltro;
import com.proyecto.servicios.model.ClienteResumenResponse;
import com.proyecto.servicios.model.PaginaResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import com.proyecto.servicios.service.clientes.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Validated
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteRegistroResponse> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        ClienteRegistroResponse response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDetalleResponse> obtenerCliente(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    @PatchMapping("/{id}/baja")
    public ResponseEntity<GenericResponse> darDeBajaCliente(@PathVariable Long id) {
        clienteService.darDeBaja(id);
        GenericResponse response = new GenericResponse();
        response.setCodigo(200);
        response.setMensaje("Cliente dado de baja exitosamente.");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ClienteDetalleResponse actualizar(@PathVariable Long id,
            @Valid @RequestBody ClienteActualizacionRequest request) {
        return clienteService.actualizar(id, request);
    }

    @GetMapping
    public PaginaResponse<ClienteResumenResponse> buscar(
            @RequestParam(required = false) @Size(max = 50, message = "El nombre admite máximo 50 caracteres") String nombre,
            @RequestParam(required = false) @Size(max = 50, message = "El apellido paterno admite máximo 50 caracteres") String apellidoPaterno,
            @RequestParam(required = false) @Size(max = 50, message = "El apellido materno admite máximo 50 caracteres") String apellidoMaterno,
            @RequestParam(required = false) @Size(max = 18, message = "La CURP admite máximo 18 caracteres") String curp,
            @RequestParam(required = false) @Size(max = 13, message = "El RFC admite máximo 13 caracteres") String rfc,
            @RequestParam(required = false) @Size(max = 100, message = "El correo admite máximo 100 caracteres") String correo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) @Size(max = 10, message = "El número de cuenta admite máximo 10 caracteres") String numeroCuenta,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return clienteService.buscar(new ClienteFiltro(nombre, apellidoPaterno, apellidoMaterno,
                curp, rfc, correo, activo, fechaDesde, fechaHasta, numeroCuenta), pageable);
    }
}
