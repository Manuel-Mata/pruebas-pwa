package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.ClienteRegistroRequest;
import com.proyecto.servicios.model.ClienteRegistroResponse;
import com.proyecto.servicios.model.ClienteDetalleResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.service.clientes.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
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
}
