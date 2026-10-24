# Sistema de operaciones e incidencias bancarias

Configuración de integración continua: [Jenkins local](docs/jenkins.md).

Proyecto de aprendizaje con Java 17, Spring Boot y SQL. Permite consultar cuentas,
transferir dinero, consultar el historial y detectar operaciones para revisión.

## Arrancar y probar

```sh
./mvnw spring-boot:run
```

Ejecuta desde la carpeta del proyecto. En Windows usa `mvnw.cmd`.
Maven Wrapper descarga Maven y las dependencias la primera vez; necesita Internet.
No necesitas instalar Maven globalmente ni instalar un servidor SQL.
El servidor escucha en `127.0.0.1:8080`. Ctrl+C lo detiene.

Abre en el navegador:

- http://localhost:8080/api/cuentas
- http://localhost:8080/api/transferencias
- http://localhost:8080/api/incidencias

Para transferir, desde otra terminal:

```sh
curl -i http://localhost:8080/api/transferencias \
  -H 'Content-Type: application/json' \
  -d '{"cuentaOrigenId":1,"cuentaDestinoId":2,"importe":20.00}'
```

Al crear la base por primera vez, Ana (ID 1) tiene 100 EUR y Luis (ID 2), 50 EUR.
La petición devuelve sus nuevos saldos: 80 y 70 EUR. Repetir el POST ejecuta otra
transferencia. Reiniciar ya NO restablece los saldos ni borra el historial.
Los saldos de la versión anterior, guardados solo en memoria, no se migran.

Una petición inválida devuelve HTTP 400 con `error`; una cuenta inexistente, 404.
Los importes admiten hasta dos decimales y 15 dígitos enteros.
GET /api/transferencias devuelve ID, cuentas de origen y destino, importe, moneda y fecha.

## Dónde están los datos

H2 es el motor SQL integrado. Guarda los datos en `data/banco.mv.db`, relativo a la
carpeta desde la que arrancas. La carpeta `data/` está excluida de Git.
No arranques dos procesos sobre el mismo archivo; detén el servidor antes de volver
 a iniciarlo. Los tests usan bases aisladas y no modifican tus datos de demostración.

- `src/main/resources/schema.sql`: crea las tablas si no existen.
- `src/main/resources/data.sql`: crea las cuentas de ejemplo solo si faltan.
- `application.properties`: configura la conexión a H2 en archivo.

| Tabla | Contenido |
|---|---|
| `cuentas` | ID, titular, saldo decimal y moneda |
| `transferencias` | ID, dos referencias a cuentas, importe, moneda y fecha |

Las referencias son **claves foráneas**: impiden registrar transferencias que apunten
a cuentas inexistentes. Los importes usan DECIMAL en SQL y BigDecimal en Java.
Estos scripts son una inicialización sencilla; los futuros cambios de esquema
necesitarán migraciones, no basta con editar CREATE TABLE IF NOT EXISTS.

## Recorrido de una transferencia

```text
Cliente → BancoController → BancoService (@Transactional)
                              → CuentaRepository: lee y bloquea las cuentas en SQL
                              → ServicioTransferencias: aplica las reglas Java
                              → CuentaRepository: guarda ambos saldos
                              → TransferenciaRepository: registra la transferencia
        ← respuesta JSON      ← confirma la transacción
```

`Repository` es la capa de acceso a datos. Usa `JdbcTemplate`, una herramienta de Spring
para ejecutar SQL con parámetros y convertir filas a objetos. El SQL está explícito
en los repositorios para poder estudiarlo; no estamos usando JPA/Hibernate.

`@Transactional` hace que las dos actualizaciones y el registro se confirmen juntos.
Si falla una escritura, se revierten todas. Las cuentas se bloquean con SELECT FOR UPDATE
en orden de ID para que peticiones simultáneas no gasten el mismo saldo ni bloqueen
las cuentas en órdenes opuestos. Ya no dependemos de `synchronized` en Java.

`Cuenta` y `ServicioTransferencias` conservan la lógica inicial. BancoService ya no
almacena un mapa de cuentas: obtiene objetos desde SQL para cada operación.

## Spring que ya conocemos

- `Main` y `@SpringBootApplication`: arrancan Spring y el servidor.
- `@Bean`: registra nuestro `ServicioTransferencias` en Spring.
- `@Service` y `@Repository`: componentes que Spring crea e inyecta mediante constructores.
- `@RestController`, `@GetMapping` y `@PostMapping`: reciben peticiones HTTP en rutas concretas.
- `@RequestBody` y `@Valid`: convierten y validan el JSON de entrada.
- `SolicitudTransferencia` y `CuentaRespuesta`: objetos de datos (records).
- `ApiExceptionHandler`: convierte las excepciones previstas en errores HTTP.

## Pruebas

```sh
./mvnw test
```

También sirve `bash scripts/test.sh`. Para ejecutar tests y empaquetar: `./mvnw verify`.

- `ServicioTransferenciasTest`: reglas Java sin Spring.
- `BancoApiTest`: rutas, JSON y errores, con Spring, MockMvc y SQL en memoria.
- `BancoPersistenciaTest`: saldos e historial SQL, rollback ante un fallo en la última
  escritura y transferencias concurrentes que no pueden gastar dos veces el saldo.
- `ReinicioPersistenciaTest`: cierra y reabre Spring sobre una base en archivo temporal
  y comprueba que se conservan saldos e historial sin duplicar las cuentas iniciales.

## POM y alcance

Spring Boot coordina las versiones. Web aporta HTTP; Validation valida datos; JDBC aporta
el acceso SQL y las transacciones; H2 es la base; Test incluye JUnit y utilidades de pruebas.
Surefire ejecuta los tests y el plugin de Spring Boot permite arrancar y empaquetar.

Es una demostración local sin autenticación ni interfaz gráfica. La detección incluye por ahora una sola regla.
El historial aún no tiene paginación y repetir una petición ejecuta una nueva transferencia.
H2 permite aprender SQL y transacciones sin instalar Oracle o SQL Server; una migración
a esos motores requeriría adaptar y verificar el esquema y las consultas.

Referencias: [inicialización SQL de Spring Boot](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)
y [documentación de H2](https://h2database.com/html/features.html).

## Primera regla de detección e incidencias

`ReglaImporteElevado` detecta transferencias EUR **estrictamente superiores** al umbral
`banco.deteccion.umbral-eur=50.00` de `application.properties`. Es una cifra didáctica
para nuestras cuentas pequeñas, no un criterio bancario real ni una prueba de fraude.
Puedes cambiarla y reiniciar. La regla no convierte divisas y solo evalúa EUR.

Una transferencia válida de 60 EUR se realiza y crea una incidencia con código
`IMPORTE_ELEVADO`, motivo con importe y umbral aplicado, prioridad `MEDIA` y estado
`PENDIENTE`. Una de 50 EUR o menos no genera incidencia. Una operación rechazada
(por ejemplo, sin saldo) tampoco genera una incidencia bajo esta regla.

```sh
curl -i http://localhost:8080/api/transferencias \
  -H 'Content-Type: application/json' \
  -d '{"cuentaOrigenId":1,"cuentaDestinoId":2,"importe":60.00}'

curl http://localhost:8080/api/incidencias
```

Consulta primero los saldos: el origen debe disponer de al menos 60 EUR. Con los saldos
iniciales, Ana queda con 40 EUR y Luis con 110 EUR. Los saldos anteriores persisten;
reiniciar no los repone. Por ahora, para probar, puedes transferir en el otro sentido
si esa cuenta tiene saldo suficiente.

La nueva tabla `incidencias` referencia `transferencias` mediante una clave foránea.
La combinación transferencia/regla es única. `TransferenciaRepository.registrar`
ahora devuelve el ID generado por SQL, para vincular la incidencia exacta.

`BancoService` registra la transferencia, evalúa la regla y guarda la incidencia
si procede, todo dentro de la misma transacción. Si falla el guardado de la incidencia,
se revierten también la transferencia y los saldos. Detectar una operación no la
rechaza; un fallo técnico de persistencia sí impide confirmar el conjunto.

La tabla nueva se crea al arrancar incluso sobre un archivo H2 existente. Las tablas
anteriores no se borran. No se analizan retroactivamente las transferencias antiguas.
GET /api/incidencias devuelve las incidencias persistidas. GET /api/incidencias/{id}
devuelve una incidencia concreta como objeto JSON o 404 si no existe. Por ejemplo,
`curl -i localhost:8080/api/incidencias/1` (usa un ID del listado). Aún no hay paginación.

Para empezar a revisar una incidencia pendiente:

```sh
curl -i -X POST localhost:8080/api/incidencias/1/revision
```

Devuelve la incidencia en estado `EN_REVISION`. Si no existe, responde 404; si ya
está en revisión o resuelta, 409. La tabla `cambios_incidencia` guarda el estado
anterior, el nuevo y la fecha del cambio, en la misma transacción que la actualización.
La fecha original de la incidencia se conserva. La tabla se crea al reiniciar sin
borrar datos existentes.

Para resolver una incidencia en revisión, envía un comentario obligatorio (máximo
1000 caracteres):

```sh
curl -i localhost:8080/api/incidencias/1/resolucion \
  -H 'Content-Type: application/json' \
  -d '{"comentario":"Transferencia revisada; operación justificada."}'
```

Devuelve la incidencia en estado `RESUELTA`. Responde 400 si falta el comentario,
está vacío o supera el límite, 404 si no existe la incidencia y 409 si no estaba
en revisión. El comentario queda en `cambios_incidencia`, junto al cambio y su
fecha; no se incluye en el JSON de la incidencia. Todo se confirma en una misma
transacción. Al arrancar, un `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` añade la
columna a bases existentes conservando sus registros.

Consulta los cambios de una incidencia, del más antiguo al más reciente:

```sh
curl -i localhost:8080/api/incidencias/1/historial
```

Cada cambio contiene `id`, `incidenciaId`, `estadoAnterior`, `estadoNuevo`, `fecha`
y `comentario` (null al iniciar la revisión). Una incidencia sin cambios devuelve
`[]`; una inexistente, 404. La fecha de creación se consulta en
`GET /api/incidencias/{id}`: no es un cambio de estado del historial.

Pruebas añadidas: límites del umbral, regla configurable, consulta HTTP, operaciones
rechazadas, rollback si falla la incidencia y persistencia de la incidencia al reiniciar.
