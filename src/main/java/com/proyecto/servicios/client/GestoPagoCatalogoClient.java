package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.catalogo.CatalogoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "gestoPagoCatalogo",
        url = "${gestopago.catalogo.url}",
        configuration = GestoPagoCatalogoFeignConfig.class)
public interface GestoPagoCatalogoClient {

    @GetMapping("/sistema/service/getProductList.do")
    CatalogoResponse obtenerCatalogo(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}