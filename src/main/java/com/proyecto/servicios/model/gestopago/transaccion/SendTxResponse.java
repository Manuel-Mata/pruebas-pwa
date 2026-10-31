package com.proyecto.servicios.model.gestopago.transaccion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.proyecto.servicios.model.gestopago.MensajeDto;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Respuesta XML de POST /sistema/service/sendTx.do.
 * Un ID_TX = "-1" o MENSAJE.CODIGO distinto de "01" indica que la transacción
 * no se aplicó (ver MENSAJE.TEXTO para el detalle).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class SendTxResponse {

    @JacksonXmlProperty(localName = "ID_TX")
    private String idTx;

    @JacksonXmlProperty(localName = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @JacksonXmlProperty(localName = "SALDO")
    private BigDecimal saldo;

    @JacksonXmlProperty(localName = "COMISION")
    private BigDecimal comision;

    // Campos "_F": version formateada para mostrar (puede traer coma de miles,
    // ej. "19,259.77"), por eso son String y no BigDecimal -- SALDO y COMISION
    // (sin "_F") son los que se usan para cualquier calculo.
    @JacksonXmlProperty(localName = "SALDO_F")
    private String saldoF;

    @JacksonXmlProperty(localName = "COMISION_F")
    private String comisionF;

    @JacksonXmlProperty(localName = "FECHA")
    private String fecha;

    @JacksonXmlProperty(localName = "MONTO")
    private BigDecimal monto;

    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeDto mensaje;

    public boolean fueExitosa() {
        return mensaje != null && "01".equals(mensaje.getCodigo()) && !"-1".equals(idTx);
    }
}
