package com.proyecto.servicios.service.transacciones;

import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.transaccion.TransaccionSolicitudRequest;

public interface TransaccionService {

    /**
     * Procesa un pago PostPago contra GestoPago (sendTx.do): valida que la cuenta
     * pertenezca al cliente autenticado (correoAutenticado, del JWT), su saldo,
     * arma y encripta el payload a partir del catálogo, envía la transacción y
     * persiste el resultado.
     */
    TransaccionResponse procesar(TransaccionSolicitudRequest request, String correoAutenticado);
}
