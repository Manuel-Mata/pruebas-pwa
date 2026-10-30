package com.proyecto.servicios.security.gestopago;

import com.proyecto.servicios.exception.EncriptacionGestoPagoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class GestoPagoEncryptionServiceImpl implements GestoPagoEncryptionService {

    private static final String ALGORITMO_CIFRADO = "AES/CBC/PKCS5Padding";
    private static final String ALGORITMO_LLAVE = "AES";

    @Value("${gestopago.encriptacion.secret-key}")
    private String secretKeyBase64;

    @Value("${gestopago.encriptacion.iv}")
    private String ivBase64;

    @Override
    public String encriptar(String jsonPayload) {
        try {
            byte[] key = Base64.getDecoder().decode(secretKeyBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);

            Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRADO);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, ALGORITMO_LLAVE),
                    new IvParameterSpec(iv));

            byte[] encriptado = cipher.doFinal(jsonPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encriptado);
        } catch (Exception e) {
            throw new EncriptacionGestoPagoException(
                    "No fue posible encriptar el payload para GestoPago.", e);
        }
    }
}
