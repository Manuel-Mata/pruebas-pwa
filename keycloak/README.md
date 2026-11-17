# Keycloak - Ecosistema de Comercio Digital (Unidad 2)

Configuracion de identidad del proyecto. Levanta Keycloak en Docker con el realm
`gestopago` ya configurado (clients `web-app`, `mobile-app` y `backend-service`).

## Requisitos
- Docker Desktop

## Levantar
Desde esta carpeta:

    docker compose up -d

Si ya existe un contenedor viejo con el mismo nombre, borralo primero:

    docker rm -f keycloak-pwa

## Acceso
- Consola admin: http://localhost:8080  (admin / admin, solo desarrollo)
- Realm: `gestopago`

## Clients
- `web-app`        - publico + PKCE (SPA React). Redirect: http://localhost:5173/*
- `mobile-app`     - publico + PKCE (Flutter).   Redirect: com.pwa.mobile://login
- `backend-service`- confidencial + Service Account (M2M / client credentials)

## Endpoints del realm
- Issuer:    http://localhost:8080/realms/gestopago
- Token:     http://localhost:8080/realms/gestopago/protocol/openid-connect/token
- JWKS:      http://localhost:8080/realms/gestopago/protocol/openid-connect/certs
- Discovery: http://localhost:8080/realms/gestopago/.well-known/openid-configuration

## Notas
- `start-dev` usa H2 en memoria: la configuracion se recarga desde
  `realm-gestopago.json` en cada arranque.
- El secreto de `backend-service` incluido en el export es SOLO para desarrollo
  local. 
- El usuario de prueba `cliente.demo` no viene en el export (Partial export no
  incluye usuarios). Se debe crear manual en Users si lo necesitas.
