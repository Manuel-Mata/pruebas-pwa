package com.proyecto.servicios.model.gestopago.catalogo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class CatalogoResponse {

    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeDto mensaje;

    @JacksonXmlElementWrapper(localName = "PRODUCTOS")
    @JacksonXmlProperty(localName = "producto")
    private List<ProductoDto> productos = new ArrayList<>();
}