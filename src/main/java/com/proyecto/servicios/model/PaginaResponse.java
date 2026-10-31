package com.proyecto.servicios.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter @Builder
public class PaginaResponse<T> {
    private List<T> contenido;
    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;
    private boolean primera;
    private boolean ultima;

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return PaginaResponse.<T>builder()
                .contenido(page.getContent())
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .primera(page.isFirst())
                .ultima(page.isLast())
                .build();
    }
}
