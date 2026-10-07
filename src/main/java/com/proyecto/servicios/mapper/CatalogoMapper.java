package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.mongo.ProductoCatalogo;
import com.proyecto.servicios.model.gestopago.catalogo.ProductoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CatalogoMapper {

    @Mapping(target = "legend", source = "legend", qualifiedByName = "limpiarTexto")
    ProductoCatalogo toProducto(ProductoDto dto);

    List<ProductoCatalogo> toProductos(List<ProductoDto> dtos);

    @Named("limpiarTexto")
    default String limpiarTexto(String texto) {
        return texto == null ? null : texto.trim();
    }
}