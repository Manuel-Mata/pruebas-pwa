package com.proyecto.servicios.client;

import com.proyecto.servicios.model.externo.PostaliCpResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "postaliClient", url = "${postali.url}")
public interface PostaliClient {

    @GetMapping("/api/v1/mx/cp/{codigo}")
    PostaliCpResponse consultarCodigoPostal(@PathVariable("codigo") String codigo);
}
