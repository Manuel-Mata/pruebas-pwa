package com.proyecto.servicios.client;

import com.proyecto.servicios.exception.AutenticacionGestoPagoException;
import com.proyecto.servicios.exception.RespuestaNoExitosaGestoPagoException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

public class GestoPagoCatalogoErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            return new AutenticacionGestoPagoException(
                    "GestoPago rechazó el token o este expiró (HTTP " + status + ")");
        }
        return new RespuestaNoExitosaGestoPagoException(
                "GestoPago respondió con un estado no exitoso (HTTP " + status + ")", null);
    }
}