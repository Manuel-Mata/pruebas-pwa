package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    boolean existsByCorreo(String correo);

    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);
    Optional<Cliente> findByCorreo(String correo);
}
