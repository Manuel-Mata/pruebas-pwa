package com.proyecto.servicios.repositorys.transacciones;

import com.proyecto.servicios.entity.transacciones.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByUpc(String upc);

    // Para el proceso de reconciliación: transacciones que quedaron en espera
    // de confirmTx.do (timeout de 62s superado sin respuesta de sendTx).
    List<Transaccion> findByEstado(EstadoTransaccion estado);
}
