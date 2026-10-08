package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreo(String correo);
}
