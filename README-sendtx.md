# Integración de pagos PostPago GestoPago (sendTx.do + confirmTx.do)

## Objetivo

Consumir el servicio de pagos PostPago de GestoPago (`POST /sistema/service/sendTx.do`) para
descontar el saldo de una cuenta a cambio de un producto/servicio del catálogo ya sincronizado
(`README-catalogo.md`), cumpliendo con los requisitos propios de un flujo de dinero real: payload
cifrado, prevención de cobro duplicado, validación de saldo/estatus de cuenta, y reconciliación
automática cuando GestoPago no responde a tiempo (`POST /sistema/service/confirmTx.do`).

## Endpoint expuesto

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/transacciones` | Procesa un pago PostPago contra una cuenta. Requiere un `idempotencyKey` único por intento. |

### Request (`TransaccionSolicitudRequest`)

| Campo | Tipo | Notas |
|---|---|---|
| `cuentaId` | `Long` | `@NotNull @Positive` |
| `idServicio` | `Integer` | debe existir en el catálogo sincronizado |
| `idProducto` | `Integer` | `@NotNull`, junto con `idServicio` identifica el producto |
| `referenciaPago` | `String` | teléfono o referencia, según el producto (ver más abajo) |
| `idempotencyKey` | `String` | `@NotBlank`; la genera el cliente que llama a este endpoint; se usa como `upc` |

### Response (`TransaccionResponse`)

`id`, `upc`, `estado`, `monto`, `idTxGestoPago`, `numAutorizacion`, `mensaje`, `fechaSolicitud`.

## Arquitectura (capas)

```
TransaccionController                     → REST (POST /transacciones)
  └─ TransaccionService (orquestador)      → arma el payload, cifra, llama a GestoPago, decide el resultado
      ├─ TransaccionPersistenciaService    → TODA la escritura en BD (transacciones @Transactional cortas)
      ├─ GestoPagoTransaccionClient        → Feign client hacia sendTx.do / confirmTx.do (form-urlencoded)
      ├─ GestoPagoEncryptionService        → AES-256/CBC/PKCS5Padding del payload JSON
      └─ GestoPagoAuthorizationHeaderProvider → reutiliza el mismo token Bearer del catálogo/auth

ReconciliacionTransaccionService (@Scheduled) → revisa transacciones EN_RECONCILIACION y llama a confirmTx.do
```

## Decisiones técnicas y por qué

**Por qué hay un servicio orquestador (`TransaccionService`) separado del servicio
transaccional (`TransaccionPersistenciaService`).**
Spring solo aplica el proxy de `@Transactional` cuando el método se llama *desde fuera* del bean.
Si toda la lógica (validar saldo, llamar a GestoPago, guardar resultado) estuviera en una sola clase
con un único método `@Transactional`, la transacción de base de datos quedaría abierta durante toda
la llamada HTTP a GestoPago (hasta 65s de timeout) — bloqueando el `PESSIMISTIC_WRITE` sobre la
cuenta todo ese tiempo. Por eso `TransaccionPersistenciaService` es un bean distinto, con métodos
`@Transactional` cortos (solo lectura/escritura en BD), y `TransaccionServiceImpl` es quien orquesta:
abre una transacción corta para registrar el intento, **cierra esa transacción**, y solo entonces
hace la llamada HTTP a GestoPago.

**Por qué la prevención de cobro duplicado usa un único `upc`.**
El `idempotencyKey` que manda el cliente de la API se usa, sin transformarlo, como:
1. Restricción `UNIQUE` en la columna `upc` de la tabla `transacciones` (deduplicación local).
2. El campo `upc` literal que se manda a GestoPago en el payload de `sendTx.do` (deduplicación en su
   lado: 15 min para recargas/entretenimiento, 24h para pagos con referencia, según su documentación).

Un mismo `upc` nunca puede generar dos cargos, ni local ni en GestoPago.

**Por qué se guarda el payload original sin cifrar (`payload_original`) en vez de solo guardar
la hora u otros campos sueltos.**
`confirmTx.do` necesita reenviarse con **exactamente** el mismo `upc` y los mismos datos que se
mandaron en el `sendTx.do` original. Reconstruir el payload desde campos sueltos (hora local, etc.)
arriesgaba generar un JSON distinto byte a byte. Guardar el JSON completo (antes de cifrarlo) y
volver a cifrarlo al reconciliar elimina ese riesgo.

**Por qué el orden de validación en `registrarIntento` es upc → cuenta (con lock) → activa → saldo.**
- `upc` se revisa primero porque es la validación más barata y evita tomar el lock de la cuenta para
  un reintento que de todas formas se va a rechazar.
- El `SELECT ... FOR UPDATE` (`buscarConLockParaActualizar`, ya usado en el proyecto para
  `generarNumeroCuenta`) se toma antes de validar estatus/saldo para que dos solicitudes concurrentes
  sobre la misma cuenta no puedan leer el mismo saldo "viejo" y ambas pasar la validación.

**Por qué el cliente Feign de transacciones no reintenta automáticamente
(`Retryer.NEVER_RETRY`).**
A diferencia de una consulta (catálogo), un pago nunca debe reintentarse solo ante una falla de red:
si la petición sí llegó a GestoPago pero la respuesta se perdió, reintentar duplicaría el cargo. Por
eso, ante un timeout, la transacción se marca `EN_RECONCILIACION` y es el job de reconciliación —no
un retry automático— quien decide el resultado real llamando a `confirmTx.do`.

**Por qué el `read-timeout` del cliente (65000 ms) es mayor al límite documentado de GestoPago
(62s).**
Así se distingue "GestoPago todavía no respondió, hay que confiar en que sí recibió la petición" de
"la conexión se cayó antes de que GestoPago pudiera procesar nada". Pasado ese margen, se asume que
la transacción puede haberse aplicado del lado de GestoPago y se reconcilia en vez de reintentar.

**Por qué `tipoFront` decide si el payload lleva `telefono` o `referencia`+`montoPago`.**
Se confirmó en la documentación de GestoPago que `tipoFront == 1` corresponde a recarga telefónica
(payload con campo `telefono`); todo lo demás se documenta como pago con referencia (`referencia` +
`montoPago`). **Esto queda marcado explícitamente como un supuesto a validar con más casos de
prueba reales** — no se encontró en la colección de Postman un ejemplo de todos los valores
posibles de `tipoFront`.

**Por qué el campo `unidad` es una propiedad de configuración (`gestopago.transaccion.unidad`) y
no algo calculado.**
No se encontró en la documentación disponible una regla de negocio para derivarlo; se deja como
valor fijo configurable por ambiente, documentado como supuesto pendiente de confirmar con el
profesor o con documentación adicional de GestoPago.

## Manejo de errores

Las excepciones propias del dominio de transacciones extienden `TransaccionException` (nueva, con su
propio `HttpStatus`), separada de `CatalogoException` (Bloque 1). Los errores de comunicación con
GestoPago (timeout, red) **se reutilizan** de `CatalogoException` en vez de duplicarse, porque son el
mismo tipo de falla de integración ya resuelta ahí.

| Excepción | Extiende | Cuándo ocurre | HTTP |
|---|---|---|---|
| `TransaccionDuplicadaException` | `TransaccionException` | Ya existe una transacción con ese `upc` | 409 |
| `CuentaNoEncontradaException` | `OnboardingException` (reutilizada) | `cuentaId` no existe | 404 |
| `OperacionNoAutorizadaException` | `TransaccionException` | La cuenta no pertenece al cliente autenticado (correo del JWT) | 403 |
| `CuentaInactivaException` | `TransaccionException` | La cuenta no está `ACTIVA` | 403 |
| `SaldoInsuficienteException` | `TransaccionException` | El saldo es menor al monto del producto | 402 |
| `EncriptacionGestoPagoException` | `TransaccionException` | Falla al cifrar/serializar el payload | 500 |
| `TimeoutGestoPagoException` | `CatalogoException` (reutilizada) | GestoPago no responde dentro del timeout; la transacción queda `EN_RECONCILIACION` | 504 |
| `ComunicacionGestoPagoException` | `CatalogoException` (reutilizada) | Error de red/HTTP no mapeado a los anteriores; la transacción queda `FALLIDA` | 503 |
| `CatalogoNoEncontradoException` | `CatalogoException` (reutilizada) | `idServicio`/`idProducto` no existen en el catálogo sincronizado | 404 |

Todas se centralizan en `GlobalExceptionHandler` (`@RestControllerAdvice`), con un `@ExceptionHandler`
dedicado para `TransaccionException` que respeta el mismo patrón que ya existía para `CatalogoException`
y `OnboardingException` (respuesta `GenericResponse<Void>` genérica, sin exponer detalles internos).

## Idempotencia y manejo de saldo

- Un mismo `idempotencyKey`/`upc` nunca produce dos cargos: se valida con `UNIQUE` en BD antes de
  intentar nada, y es el mismo valor que GestoPago usa para su propia deduplicación.
- El saldo se descuenta **solo** cuando GestoPago confirma éxito (`fueExitosa() == true`), ya sea en
  la respuesta inmediata de `sendTx.do` o, si hubo timeout, en la confirmación posterior de
  `confirmTx.do`. Nunca se descuenta por adelantado.
- El descuento de saldo usa el mismo lock pesimista (`buscarConLockParaActualizar`) que la validación
  inicial, para que dos confirmaciones concurrentes no puedan descontar el mismo saldo dos veces.
- Antes de cualquier otra validación, se verifica que la cuenta pertenezca al cliente autenticado:
  se resuelve el `Usuario` a partir del correo del JWT (`Authentication.getName()`, inyectado en el
  controller) y se compara su `cliente.id` contra el `cliente.id` de la cuenta solicitada. Nunca se
  confía en un `clienteId` que pudiera mandar el propio request — no existe ese campo. Un mismatch
  lanza `OperacionNoAutorizadaException` (403) antes de tocar el estatus o el saldo de la cuenta.

## Reconciliación por timeout

`ReconciliacionTransaccionServiceImpl.reconciliarPendientes()` corre cada
`gestopago.transaccion.reconciliacion-rate-ms` (5 min por defecto, con 1 min de retraso inicial) y, por
cada transacción en estado `EN_RECONCILIACION`:

1. Si ya alcanzó `gestopago.transaccion.reconciliacion-max-intentos` (5 por defecto) sin una respuesta
   concluyente, se marca `FALLIDA` directamente y se deja constancia en el log para revisión manual.
2. Si no, vuelve a cifrar el mismo `payload_original` guardado y llama a `confirmTx.do` con el mismo
   `upc`. Según la respuesta:
   - Éxito → `EXITOSA`, se descuenta el saldo.
   - GestoPago confirma que no se aplicó → `FALLIDA`.
   - Sigue sin respuesta concluyente → se incrementa el contador de intentos y se reintenta en el
     siguiente ciclo.

## Logs

`TransaccionServiceImpl.procesar()` registra explícitamente inicio y fin de cada invocación
(`log.info`), igual que `CatalogoServiceImpl` en el Bloque 1, además de advertencias y errores en
cada escenario (timeout, error de comunicación, transacción fallida, agotamiento de reintentos de
reconciliación) y el resultado final de cada transacción (éxito con el saldo resultante, o motivo de
falla). En ningún mensaje se incluye el payload cifrado, el `random`/`signed`, ni el token de
autorización.

## Configuración

| Propiedad | Dónde | Qué es |
|---|---|---|
| `gestopago.transaccion.url` | `application-local.properties` | Base URL de GestoPago para `sendTx.do`/`confirmTx.do` |
| `gestopago.transaccion.connect-timeout-ms` / `read-timeout-ms` | `application-local.properties` | Timeouts del cliente Feign (65000 ms de lectura, por encima de los 62s documentados por GestoPago) |
| `gestopago.transaccion.unidad` | `application-local.properties` | Valor fijo requerido en el payload (ver "supuestos pendientes" abajo) |
| `gestopago.transaccion.reconciliacion-rate-ms` | `application-local.properties` | Frecuencia del job de reconciliación (300000 ms = 5 min) |
| `gestopago.transaccion.reconciliacion-max-intentos` | `application-local.properties` | Intentos máximos antes de marcar `FALLIDA` manualmente (5) |
| `gestopago.encriptacion.secret-key` / `gestopago.encriptacion.iv` | `application-local.properties` | Llave e IV AES-256 (Base64); por ahora el par de prueba documentado por GestoPago — nunca hardcodeados en código, nunca commiteados con un valor real |
| `gestopago.auth.*` | `application-local.properties` | Credenciales del distribuidor, reutilizadas del flujo de autenticación ya existente |

Todas estas propiedades siguen la misma convención `${VARIABLE_DE_ENTORNO:valor-por-defecto}` que ya
usa el resto del proyecto, y viven únicamente en `application-local.properties` (ignorado por Git).

## Pruebas

23 pruebas unitarias con Mockito, sin tocar red ni base de datos real, distribuidas en 3 clases:

- **`TransaccionPersistenciaServiceImplTest`** (11): registro de intento válido, `upc` duplicado,
  cuenta inexistente, **cuenta de otro cliente (403)**, **usuario autenticado no existe (403)**, cuenta
  inactiva, saldo insuficiente, confirmación exitosa descuenta saldo, marcar fallida no toca el saldo,
  marcar en reconciliación incrementa intentos, confirmación exitosa por reconciliación descuenta saldo.
- **`TransaccionServiceImplTest`** (7): producto inexistente en catálogo, armado de payload con
  `telefono` (`tipoFront == 1`), armado de payload con `referencia`+`montoPago` (otro `tipoFront`),
  confirmación exitosa cuando GestoPago responde código `01`, marcar fallida cuando no responde `01`,
  marcar en reconciliación y lanzar `TimeoutGestoPagoException` ante un `RetryableException`, marcar
  fallida y lanzar `ComunicacionGestoPagoException` ante un `FeignException`.
- **`ReconciliacionTransaccionServiceImplTest`** (5): no hace nada sin pendientes, confirma exitosa
  cuando GestoPago responde `01`, marca fallida cuando GestoPago confirma que no se aplicó, registra
  intento fallido cuando `confirmTx.do` no responde, marca fallida directamente al agotar los
  intentos máximos.

## Prueba en vivo (30/oct/2026)

Se probó `POST /transacciones` end-to-end contra el servidor real de GestoPago
(`https://gestopago.portalventas.net`), con una cuenta real (saldo `$5000.50`) y un producto real del
catálogo sincronizado (ABIB 50, `idServicio=2284`, `idProducto=14293`, `tipoFront=1`). Resultado:
`200 OK`, `estado: EXITOSA`, `idTxGestoPago`/`numAutorizacion` devueltos por GestoPago, y el saldo de
la cuenta bajó correctamente a `$4950.50`. También se verificó en el camino:
- El job de reconciliación se activa correctamente ante un timeout real (se probó sin querer, antes
  de configurar bien la URL del servidor — la transacción quedó `EN_RECONCILIACION` como se esperaba).
- La prevención de duplicados funciona: reenviar el mismo `idempotencyKey` devuelve `409 Conflict`
  (`TransaccionDuplicadaException`) sin volver a llamar a GestoPago.

Esta prueba también sacó a la luz dos bugs reales, ya corregidos:

**1. `Transaccion`/`TransaccionRepository` nunca se registraron en la configuración de JPA.**
`ConfigDB.java` define manualmente (por el datasource dual del proyecto) qué paquetes escanea
`@EnableJpaRepositories` y `setPackagesToScan(...)`; el paquete `repositorys.transacciones` /
`entity.transacciones` se quedó fuera al construirlos, y como las pruebas unitarias usan Mockito (no
levantan Spring), nadie lo detectó hasta arrancar la aplicación real
(`NoSuchBeanDefinitionException: TransaccionRepository`). Corregido agregando ambos paquetes a
`ConfigDB`.

**2. `SendTxResponse.saldoF`/`comisionF` mapeados como `BigDecimal` en vez de `String`.**
La respuesta real de `sendTx.do` incluye campos "formateados para mostrar" (`SALDO_F`, `COMISION_F`)
que pueden traer coma de miles, por ejemplo `"19,259.77"` — un valor que `BigDecimal` no puede
parsear directamente, lo que rompía la deserialización de **toda** la respuesta (incluida una
transacción exitosa) y la reportaba como error 503 de comunicación, aunque GestoPago ya la hubiera
aplicado correctamente. Corregido cambiando esos dos campos a `String` (los campos sin `_F`, que sí
son `BigDecimal`, son los que se usan para cualquier cálculo).

### Actualización (30/oct/2026, tarde): validación de autorización por cliente

Al revisar la actividad completa del profesor se detectó que `POST /transacciones` nunca verificaba
que la cuenta solicitada perteneciera al cliente autenticado: cualquier usuario con sesión válida
podía cobrar sobre **cualquier** cuenta existente con saldo, con solo conocer su `cuentaId`. Se agregó
la validación descrita en "Idempotencia y manejo de saldo" (`OperacionNoAutorizadaException`, 403) y se
volvió a probar en vivo:
- `POST /transacciones` con el token de `ana.roma@test.com` (cliente dueño de la cuenta 4) contra la
  cuenta 1 (de otro cliente) → `403 Forbidden`, "No tiene autorización para operar sobre esta cuenta.",
  sin llegar a tocar el saldo ni a llamar a GestoPago.
- El mismo request contra su propia cuenta (4) → `200 OK`, `estado: EXITOSA`, igual que antes del fix.

## Pendiente

- Confirmar con más casos de prueba el supuesto de que `tipoFront == 1` es el único valor que usa
  `telefono` en vez de `referencia`+`montoPago` (la prueba en vivo solo cubrió `tipoFront == 1`).
- Confirmar el origen/significado real del campo `unidad` (funcionó con el valor configurado, pero
  sigue sin una regla de negocio documentada para derivarlo en vez de ser un valor fijo).
- Probar en vivo el flujo de reconciliación real con `confirmTx.do` después de un timeout genuino
  (hasta ahora solo se confirmó que el job se activa correctamente, no su resultado final contra
  GestoPago).
