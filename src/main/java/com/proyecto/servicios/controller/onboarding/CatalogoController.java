package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.CodigoPostalRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.externo.CodigoPostalResponse;
import com.proyecto.servicios.service.catalogos.CodigoPostalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final CodigoPostalService codigoPostalService;

    @PostMapping("/codigo-postal")
    public ResponseEntity<GenericResponse<CodigoPostalResponse>> consultarCodigoPostal(
            @Valid @RequestBody CodigoPostalRequest request) {
        CodigoPostalResponse respuesta = codigoPostalService.consultar(request.getCp());
        return ResponseEntity.ok(GenericResponse.exito("Código postal consultado exitosamente.", respuesta));
    }
}
