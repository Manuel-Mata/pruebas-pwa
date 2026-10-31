package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);
    List<Cuenta> findByClienteId(Long clienteId);

    List<Cuenta> findByEstatus(EstatusCuenta estatus);
    List<Cuenta> findByClienteIdAndEstatus(Long clienteId, EstatusCuenta estatus);

    @Query(value = "SELECT lpad(CAST(nextval('seq_numero_cuenta') AS text), 10, '0')", nativeQuery = true)
    String generarNumeroCuenta();

    // Lock pesimista al debitar saldo en sendTx, para que dos solicitudes
    // concurrentes sobre la misma cuenta no la dejen en negativo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.id = :cuentaId")
    Optional<Cuenta> buscarConLockParaActualizar(Long cuentaId);
}
