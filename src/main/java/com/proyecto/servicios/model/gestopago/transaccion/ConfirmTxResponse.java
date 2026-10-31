package com.proyecto.servicios.model.gestopago.transaccion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.proyecto.servicios.model.gestopago.MensajeDto;
import lombok.Data;

/**
 * Respuesta XML de POST /sistema/service/confirmTx.do.
 * NUM_AUTORIZACION = "-1" o MENSAJE.CODIGO distinto de "01" indica que la
 * transacción consultada no se aplicó (ver MENSAJE.TEXTO para el detalle).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class ConfirmTxResponse {

    @JacksonXmlProperty(localName = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeDto mensaje;

    public boolean fueExitosa() {
        return mensaje != null && "01".equals(mensaje.getCodigo()) && !"-1".equals(numAutorizacion);
    }
}
