package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Usuario;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class UsuarioSpecification {
    private UsuarioSpecification() {}

    public static Specification<Usuario> correoContiene(String valor) {
        if (valor == null) return null;
        String patron = "%" + valor.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("correo")), patron, '\\');
    }

    public static Specification<Usuario> activo(Boolean activo) {
        if (activo == null) return null;
        return (root, query, cb) -> cb.equal(root.get("activo"), activo);
    }

    public static Specification<Usuario> deCliente(Long clienteId) {
        if (clienteId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId);
    }
}
