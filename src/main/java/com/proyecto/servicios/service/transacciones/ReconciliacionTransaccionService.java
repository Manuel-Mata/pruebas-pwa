package com.proyecto.servicios.service.transacciones;

public interface ReconciliacionTransaccionService {

    /**
     * Revisa las transacciones EN_RECONCILIACION y consulta confirmTx.do por cada una
     * para determinar su estado real. Se ejecuta periódicamente (ver @Scheduled en la
     * implementación) y también puede invocarse manualmente.
     */
    void reconciliarPendientes();
}
