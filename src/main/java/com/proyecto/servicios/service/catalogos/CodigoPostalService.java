package com.proyecto.servicios.service.catalogos;

import com.proyecto.servicios.client.PostaliClient;
import com.proyecto.servicios.exception.catalogos.CodigoPostalNoEncontradoException;
import com.proyecto.servicios.exception.catalogos.ServicioNoDisponibleException;
import com.proyecto.servicios.model.externo.CodigoPostalResponse;
import com.proyecto.servicios.model.externo.PostaliCpResponse;
import feign.FeignException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodigoPostalService {

    private final PostaliClient postaliClient;

    @Cacheable(value = "codigos_postales", key = "#codigo")
    public CodigoPostalResponse consultar(String codigo) {
        log.info("Consultando código postal a Postali: {}", codigo);
        try {
            PostaliCpResponse postaliResp = postaliClient.consultarCodigoPostal(codigo);
            CodigoPostalResponse response = new CodigoPostalResponse();
            response.setCp(postaliResp.getCp());
            response.setEstado(postaliResp.getEstado());
            response.setMunicipio(postaliResp.getMunicipio());
            if (postaliResp.getAsentamientos() != null) {
                response.setColonias(postaliResp.getAsentamientos().stream()
                        .map(PostaliCpResponse.AsentamientoResponse::getNombre)
                        .collect(Collectors.toList()));
            }
            return response;
        } catch (FeignException.NotFound e) {
            log.warn("Código postal {} no encontrado en Postali.", codigo);
            throw new CodigoPostalNoEncontradoException();
        } catch (FeignException | IllegalArgumentException e) {
            log.error("Error al consultar Postali para el cp {}: {}", codigo, e.getMessage());
            throw new ServicioNoDisponibleException();
        }
    }
}
