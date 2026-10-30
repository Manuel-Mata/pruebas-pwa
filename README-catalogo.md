# Integración de catálogo GestoPago (getProductList → MongoDB)

## Objetivo

Consumir el catálogo de productos/servicios de GestoPago (`GET /sistema/service/getProductList.do`),
guardarlo en MongoDB, y exponerlo como una API propia para que el resto de la aplicación (y, más
adelante, la integración de pagos `sendTx`) lo consulte sin depender de GestoPago en cada petición.

## Endpoints expuestos

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/catalogos/sincronizar` | Llama a GestoPago, valida la respuesta y guarda un snapshot nuevo en Mongo |
| `GET` | `/catalogos/productos?idServicio=` | Devuelve los productos del snapshot más reciente guardado en Mongo (filtro opcional por `idServicio`) |

`GET /catalogos/productos` **nunca** llama a GestoPago directamente — siempre lee de Mongo. Esto es
intencional: `getProductList.do` está limitado por GestoPago a 3 llamadas diarias (bloquean la IP si
se excede), así que cualquier consumo frecuente del catálogo (front, Swagger, otros servicios) debe
pasar por nuestra copia local.

## Arquitectura (capas)

```
CatalogoController          → REST
  └─ CatalogoService        → reglas de negocio (límite diario, validación de código de respuesta)
      ├─ GestoPagoCatalogoClient   → Feign client hacia GestoPago (XML)
      ├─ GestoPagoTokenService     → reutiliza el mismo token Bearer que ya administra la integración de autenticación (renovación automática cada hora, válido 24h)
      ├─ CatalogoMapper            → ProductoDto (XML) ↔ ProductoCatalogo (Mongo) ↔ ProductoCatalogoResponse (API)
      └─ CatalogoSnapshotRepository → Mongo
```

## Decisiones técnicas y por qué

**Por qué el token se reutiliza en vez de pedirse en cada llamada.**
GestoPago exige un JWT propio (no OAuth estándar) que dura 24h y prohíbe pedir uno nuevo por cada
transacción. El proyecto ya tenía resuelto esto para otro flujo (`GestoPagoTokenServiceImpl`, con
`@Scheduled` renovándolo); en vez de duplicar esa lógica, el catálogo reutiliza el mismo servicio de
token, pasando el mismo `idDistribuidor`/`codigoDispositivo` configurados.

**Por qué Mongo guarda historial (snapshots) en vez de sobrescribir el catálogo.**
Cada `sincronizar()` inserta un documento `CatalogoSnapshot` nuevo con fecha, en vez de actualizar uno
existente. Esto permite auditar cómo cambió el catálogo entre sincronizaciones (qué productos se
agregaron/quitaron, cambios de precio) sin perder el historial — relevante para un catálogo que un
distribuidor no controla directamente. `GET /catalogos/productos` siempre toma el snapshot más
reciente (`findFirstByOrderByFechaSincronizacionDesc`).

**Por qué se limita a 2 sincronizaciones diarias (`gestopago.catalogo.max-sincronizaciones-diarias`),
no 3.**
GestoPago bloquea la IP a partir de la 4ª llamada en un día. Se deja 1 de margen para pruebas manuales
o reintentos sin arriesgar el bloqueo.

**Por qué la respuesta es XML, no JSON.**
GestoPago responde `Content-Type: text/xml`, no JSON, pese a que el payload de petición de otros
endpoints sí es JSON (luego cifrado). Se usa `jackson-dataformat-xml` con DTOs anotados
(`CatalogoResponse`, `MensajeDto`, `ProductoDto`) para deserializar directamente, sin parseo manual de
XML.

**Por qué el GET no expone la entidad de Mongo directamente.**
`ProductoCatalogo` es la entidad de persistencia (incluye campos deprecados por GestoPago como
`hasDigitoVerificador`/`showAyuda`). `ProductoCatalogoResponse` es el DTO de salida, sin esos campos,
para no acoplar el contrato de la API a cómo se persiste internamente.

## Manejo de errores

Todas las excepciones de este flujo extienden `CatalogoException` (con su `HttpStatus` asociado) y se
centralizan en `GlobalExceptionHandler` (`@RestControllerAdvice`), sin exponer detalles internos del
error (stack traces, URLs internas) en la respuesta HTTP:

| Excepción | Cuándo ocurre | HTTP |
|---|---|---|
| `LimiteDiarioSincronizacionException` | Ya se alcanzó el máximo de sincronizaciones del día | 429 |
| `AutenticacionGestoPagoException` | No hay token vigente, o GestoPago responde 401/403 | 401 |
| `TimeoutGestoPagoException` | GestoPago no responde dentro del timeout configurado | 504 |
| `ComunicacionGestoPagoException` | Error de red/HTTP no mapeado a los anteriores | 503 |
| `RespuestaNoExitosaGestoPagoException` | GestoPago responde, pero con `CODIGO` distinto de `01` | 502 |
| `CatalogoNoEncontradoException` | Se consulta `/catalogos/productos` sin haber sincronizado nunca | 404 |

## Logs

`CatalogoServiceImpl` registra inicio y fin de cada sincronización (`log.info`), y cada escenario de
error (`log.warn`/`log.error`) sin incluir el token ni la respuesta cruda de GestoPago en el mensaje,
para no filtrar información sensible en los logs.

## Configuración

| Propiedad | Dónde | Qué es |
|---|---|---|
| `gestopago.catalogo.url` | `application.properties` / `application-local.properties` | Base URL de GestoPago |
| `gestopago.catalogo.connect-timeout-ms` / `read-timeout-ms` | `application.properties` | Timeouts del cliente Feign |
| `gestopago.catalogo.max-sincronizaciones-diarias` | `application.properties` | Límite propio de llamadas a `getProductList.do` por día |
| `gestopago.auth.*` | `application-local.properties` | Credenciales del distribuidor (reutilizadas del flujo de autenticación ya existente) — nunca hardcodeadas en código |
| `spring.data.mongodb.uri` | `application-local.properties`, vía variable de entorno `MONGODB_URI` | Conexión a MongoDB Atlas; no se commitea la cadena real con credenciales, solo el placeholder `${MONGODB_URI:mongodb://localhost:27017/catalogo_dev}` |

## Pruebas

`CatalogoServiceImplTest` cubre con Mockito (sin tocar red real):
sincronización exitosa, límite diario alcanzado, token ausente, GestoPago responde código de error,
timeout, error de comunicación, catálogo nunca sincronizado, y filtro por `idServicio`.

## Pendiente

- ~~Prueba de integración contra la API real de GestoPago~~ **Completada (30/oct/2026)**: se
  confirmó `POST /catalogos/sincronizar` y `GET /catalogos/productos` funcionando end-to-end
  contra el servidor real (`https://gestopago.portalventas.net`), usando las credenciales de
  ejemplo documentadas en la colección de Postman del profesor (auth, encriptación AES y
  catálogo). Las credenciales se configuran como variables de entorno
  (`GESTOPAGO_AUTH_URL`, `GESTOPAGO_CATALOGO_URL`, `GESTOPAGO_ID_DISTRIBUIDOR`,
  `GESTOPAGO_CODIGO_DISPOSITIVO`, `GESTOPAGO_PASSWORD`) en `application-local.properties`, nunca
  hardcodeadas ni commiteadas.
- Esta misma base de catálogo (`idProducto`, `idServicio`, `unidad`, `tipoFront`) es la que va a
  alimentar la siguiente actividad de integración de pagos (`sendTx.do`).
