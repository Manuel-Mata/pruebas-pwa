package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.ClienteActualizacionRequest;
import com.proyecto.servicios.model.ClienteBajaRequest;
import com.proyecto.servicios.model.ClienteBusquedaRequest;
import com.proyecto.servicios.model.ClienteDetalleResponse;
import com.proyecto.servicios.model.ClienteRegistroRequest;
import com.proyecto.servicios.model.ClienteRegistroResponse;
import com.proyecto.servicios.model.ClienteResumenResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PaginaResponse;
import com.proyecto.servicios.service.clientes.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<GenericResponse<ClienteRegistroResponse>> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        ClienteRegistroResponse response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.exito(HttpStatus.CREATED.value(), "Cliente registrado exitosamente.", response));
    }

    @PostMapping("/buscar")
    public ResponseEntity<GenericResponse<?>> buscar(@Valid @RequestBody(required = false) ClienteBusquedaRequest request) {
        ClienteBusquedaRequest req = (request != null) ? request : new ClienteBusquedaRequest();
        if (req.getId() != null) {
            ClienteDetalleResponse detalle = clienteService.obtenerPorId(req.getId());
            return ResponseEntity.ok(GenericResponse.exito("Cliente encontrado exitosamente.", detalle));
        }
        PaginaResponse<ClienteResumenResponse> pagina = clienteService.buscar(req);
        return ResponseEntity.ok(GenericResponse.exito("Búsqueda de clientes realizada exitosamente.", pagina));
    }

    @PatchMapping
    public ResponseEntity<GenericResponse<ClienteDetalleResponse>> actualizar(@Valid @RequestBody ClienteActualizacionRequest request) {
        ClienteDetalleResponse response = clienteService.actualizar(request);
        return ResponseEntity.ok(GenericResponse.exito("Cliente actualizado exitosamente.", response));
    }

    @PatchMapping("/baja")
    public ResponseEntity<GenericResponse<Void>> darDeBajaCliente(@Valid @RequestBody ClienteBajaRequest request) {
        clienteService.darDeBaja(request.getId());
        return ResponseEntity.ok(GenericResponse.exito("Cliente dado de baja exitosamente."));
    }
}
