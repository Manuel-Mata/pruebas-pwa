package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.transaccion.ConfirmTxResponse;
import com.proyecto.servicios.model.gestopago.transaccion.SendTxResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "gestoPagoTransaccion",
        url = "${gestopago.transaccion.url}",
        configuration = GestoPagoTransaccionFeignConfig.class)
public interface GestoPagoTransaccionClient {

    @PostMapping(value = "/sistema/service/sendTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    SendTxResponse enviarTransaccion(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed);

    /**
     * Reconciliación: confirma el estado real de una transacción que no tuvo respuesta
     * a tiempo en sendTx.do. Se envía el MISMO payload (con el mismo upc) que se usó al
     * enviarla originalmente.
     */
    @PostMapping(value = "/sistema/service/confirmTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ConfirmTxResponse confirmarTransaccion(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam("random") String random,
            @RequestParam("signed") String signed);
}
