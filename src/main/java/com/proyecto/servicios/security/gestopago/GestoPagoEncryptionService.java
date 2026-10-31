package com.proyecto.servicios.security.gestopago;

/**
 * Encripta el payload de las peticiones a GestoPago (AES-256-CBC/PKCS5Padding,
 * como exige su documentación) antes de enviarlo en el campo "signed".
 */
public interface GestoPagoEncryptionService {

    /**
     * @param jsonPayload el JSON del payload, ya serializado por quien llama
     *                    (este servicio solo cifra, no arma ni serializa el payload).
     * @return el payload encriptado, codificado en Base64.
     */
    String encriptar(String jsonPayload);
}
