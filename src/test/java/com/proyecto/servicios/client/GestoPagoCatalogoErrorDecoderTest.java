package com.proyecto.servicios.client;

import com.proyecto.servicios.exception.AutenticacionGestoPagoException;
import com.proyecto.servicios.exception.RespuestaNoExitosaGestoPagoException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GestoPagoCatalogoErrorDecoderTest {

    private final GestoPagoCatalogoErrorDecoder decoder = new GestoPagoCatalogoErrorDecoder();

    private Response respuesta(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://localhost/prueba",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return Response.builder()
                .status(status)
                .reason("prueba")
                .request(request)
                .headers(Collections.emptyMap())
                .build();
    }

    @Test
    void estado401EsErrorDeAutenticacion() {
        assertInstanceOf(AutenticacionGestoPagoException.class, decoder.decode("metodo", respuesta(401)));
    }

    @Test
    void estado403EsErrorDeAutenticacion() {
        assertInstanceOf(AutenticacionGestoPagoException.class, decoder.decode("metodo", respuesta(403)));
    }

    @Test
    void estado500EsRespuestaNoExitosa() {
        assertInstanceOf(RespuestaNoExitosaGestoPagoException.class, decoder.decode("metodo", respuesta(500)));
    }
}