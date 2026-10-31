package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.clientes.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long minutosVigencia;

    public JwtService(@Value("${jwt.secret:}") String secreto,
                      @Value("${jwt.expiration-minutes:60}") long minutosVigencia) {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("Falta la propiedad jwt.secret");
        }
        byte[] bytes = Decoders.BASE64.decode(secreto);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret debe tener al menos 32 bytes en Base64");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.minutosVigencia = minutosVigencia;
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getCorreo())
                .claim("uid", usuario.getId())
                .claim("cid", usuario.getCliente().getId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosVigencia, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Lanza JwtException si la firma es inválida, el token está malformado o venció. */
    public Claims validar(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long segundosDeVigencia() {
        return minutosVigencia * 60;
    }
}
