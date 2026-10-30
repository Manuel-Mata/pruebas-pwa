package com.proyecto.servicios.model.gestopago.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MensajeDto{

    @JacksonXmlProperty(localName = "CODIGO")
    private String codigo;

    @JacksonXmlProperty(localName = "TEXTO")
    private String texto;

}   
