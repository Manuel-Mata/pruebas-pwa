package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.externo.CodigoPostalResponse;
import com.proyecto.servicios.service.catalogos.CodigoPostalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/catalogos")
@RequiredArgsConstructor
@Validated
public class CatalogoController {

    private final CodigoPostalService codigoPostalService;

    @GetMapping("/codigo-postal/{cp}")
    public ResponseEntity<CodigoPostalResponse> consultarCodigoPostal(
            @PathVariable @Pattern(regexp = "^\\d{5}$", message = "El código postal debe tener 5 dígitos") String cp) {
        CodigoPostalResponse respuesta = codigoPostalService.consultar(cp);
        return ResponseEntity.ok(respuesta);
    }
}
