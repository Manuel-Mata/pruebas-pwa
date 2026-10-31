package com.proyecto.servicios.service.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Cuenta;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Locale;

public final class ClienteSpecification {

    private ClienteSpecification() {}

    public static Specification<Cliente> contiene(String campo, String valor) {
        if (valor == null) return null;
        String patron = "%" + escaparLike(valor.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(campo)), patron, '\\');
    }

    public static Specification<Cliente> igual(String campo, Object valor) {
        if (valor == null) return null;
        return (root, query, cb) -> cb.equal(root.get(campo), valor);
    }

    public static Specification<Cliente> registradoDesde(OffsetDateTime desde) {
        if (desde == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desde);
    }

    public static Specification<Cliente> registradoAntesDe(OffsetDateTime limite) {
        if (limite == null) return null;
        return (root, query, cb) -> cb.lessThan(root.get("fechaRegistro"), limite);
    }

    public static Specification<Cliente> conNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null) return null;
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Cuenta> cuenta = sub.from(Cuenta.class);
            sub.select(cuenta.get("id")).where(
                    cb.equal(cuenta.get("cliente").get("id"), root.get("id")),
                    cb.equal(cuenta.get("numeroCuenta"), numeroCuenta));
            return cb.exists(sub);
        };
    }

    private static String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
