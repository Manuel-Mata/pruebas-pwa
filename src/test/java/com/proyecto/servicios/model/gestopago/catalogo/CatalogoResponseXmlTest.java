package com.proyecto.servicios.model.gestopago.catalogo;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoResponseXmlTest {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Test
    void deserializaRespuestaExitosa() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/catalogo/getProductList-muestra.xml")) {
            assertNotNull(in, "No se encontro la muestra XML");
            CatalogoResponse response = xmlMapper.readValue(in, CatalogoResponse.class);

            assertEquals("01", response.getMensaje().getCodigo());
            assertEquals(3, response.getProductos().size());

            ProductoDto abib = response.getProductos().get(0);
            assertEquals("ABIB", abib.getServicio());
            assertEquals(14302, abib.getIdProducto());
            assertEquals(0, new BigDecimal("100").compareTo(abib.getPrecio()));
            assertEquals(Boolean.FALSE, abib.getHasDigitoVerificador());

            ProductoDto kaspersky = response.getProductos().get(2);
            assertEquals(0, new BigDecimal("368.76").compareTo(kaspersky.getPrecio()));
            assertEquals(Boolean.TRUE, kaspersky.getShowAyuda());
            assertTrue(kaspersky.getLegend().contains("Gracias"));
        }
    }
}