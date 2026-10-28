<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/img/logo-countercheck-oscuro.svg">
    <img src="docs/img/logo-countercheck.svg" alt="Countercheck" width="200">
  </picture>
</p>

# Countercheck — Sistema de operaciones bancarias

Backend en **Java 17 + Spring Boot** y cliente de escritorio en **Eclipse RCP** para
simular el trabajo de back office de un banco: transferencias entre cuentas,
detección automática de operaciones sospechosas y gestión de las incidencias que
generan, con historial de cada cambio. Integración continua con **Jenkins**.

![Cliente RCP: incidencia pendiente generada por una transferencia de 70 EUR](docs/img/rcp-incidencias.png)

## Qué hace

- **Cuentas y transferencias**: saldos en SQL, importes con `BigDecimal`, validación
  de saldo, moneda y cuentas, y bloqueo de filas para evitar dobles gastos concurrentes.
- **Detección de anomalías**: una regla configurable marca las transferencias que
  superan un umbral y crea una incidencia en la misma transacción.
- **Gestión de incidencias**: flujo `PENDIENTE → EN_REVISION → RESUELTA`, resolución
  con comentario obligatorio e historial fechado de cada cambio de estado.
- **Autenticación y roles**: login con contraseña BCrypt que devuelve un token JWT;
  cualquier usuario autenticado opera, pero solo un `SUPERVISOR` resuelve incidencias.
- **Cliente de escritorio para empleados** (Eclipse RCP): login, consulta de cuentas,
  formulario de transferencias, revisión y resolución de incidencias según el rol.
- **CI con Jenkins**: cada cambio en `main` ejecuta las pruebas, publica los
  informes JUnit y archiva el JAR si todo pasa.

## Stack

| Área | Tecnologías |
|---|---|
| Backend | Java 17, Spring Boot 3.5 (Web, Validation, JDBC), API REST |
| Seguridad | Spring Security, BCrypt, JWT (jjwt) sin estado, autorización por rol |
| Datos | SQL con `JdbcTemplate` (sin ORM), H2 en archivo, transacciones y `SELECT FOR UPDATE` |
| Escritorio | Eclipse RCP (e4), SWT, cliente HTTP contra la API |
| Calidad | JUnit 5, MockMvc, pruebas de concurrencia, rollback, persistencia y seguridad (58 pruebas) |
| CI | Jenkins (Pipeline desde SCM) en Docker, Maven Wrapper, Git |

## Arquitectura

```text
Cliente RCP (empleado) ──HTTP/JSON + Bearer──► JwtAuthFilter (firma y rol del token)
                                                      │
                                                BancoController
                                                      │
                                     BancoService (@Transactional)
                                     ├─ ServicioTransferencias  (reglas de negocio en Java puro)
                                     ├─ ReglaImporteElevado     (detección)
                                     └─ Repositorios JDBC ──► H2 (cuentas, transferencias,
                                                                  incidencias, cambios_incidencia)
```

Una transferencia bloquea ambas cuentas en orden de ID, aplica las reglas, guarda
saldos y movimiento, evalúa la detección y registra la incidencia. Todo se confirma
o se revierte junto.

## Capturas

**1. El empleado consulta los saldos y registra una transferencia de 70 EUR.**

![Cuentas y formulario de transferencia](docs/img/rcp-cuentas.png)

**2. Al superar el umbral, se crea una incidencia; el empleado la revisa y la resuelve con un comentario.** El historial guarda cada cambio con su fecha.

![Incidencia resuelta con historial de cambios](docs/img/rcp-resolucion.png)

**3. Jenkins ejecuta las pruebas en cada cambio y guarda el JAR solo si todo pasa.**

![Resultados de las pruebas en Jenkins](docs/img/jenkins-tests.png)

![Ejecución de Jenkins con el JAR generado](docs/img/jenkins-ejecucion.png)

## Arrancar

Requisitos: Java 17 o superior. No hace falta instalar Maven ni una base de datos.

```sh
./mvnw spring-boot:run
```

El servidor escucha en `http://localhost:8080` (solo en local). En Windows usa
`mvnw.cmd`. Los datos se guardan en `data/banco.mv.db` y se conservan entre reinicios.
La primera vez se crean dos cuentas de ejemplo: Ana (ID 1, 100 EUR) y Luis (ID 2, 50 EUR).

Usuarios de demostración (en la base solo se guarda su hash BCrypt):

| Usuario | Contraseña | Rol |
|---|---|---|
| `empleado` | `empleado123` | `EMPLEADO` |
| `supervisor` | `supervisor123` | `SUPERVISOR` |

Los tokens se firman con la variable de entorno `JWT_SECRET`; sin ella se usa una
clave de desarrollo que solo sirve en local.

Para el cliente de escritorio, consulta [banco.desktop/README.md](banco.desktop/README.md).

## API

Todas las rutas exigen un token (`Authorization: Bearer <token>`) salvo el login.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/login` | Valida usuario y contraseña y devuelve un JWT (caduca en 1 hora) |
| GET | `/api/me` | Usuario y rol del token |
| GET | `/api/cuentas` | Cuentas y saldos |
| POST | `/api/transferencias` | Realiza una transferencia |
| GET | `/api/transferencias` | Historial de transferencias |
| GET | `/api/incidencias` | Incidencias detectadas |
| GET | `/api/incidencias/{id}` | Detalle de una incidencia |
| GET | `/api/incidencias/{id}/historial` | Cambios de estado con fecha y comentario |
| POST | `/api/incidencias/{id}/revision` | `PENDIENTE → EN_REVISION` |
| POST | `/api/incidencias/{id}/resolucion` | `EN_REVISION → RESUELTA` (comentario obligatorio, solo `SUPERVISOR`) |

Errores en JSON: `400` datos inválidos, `401` sin token o token inválido, `403` rol
sin permiso, `404` recurso inexistente, `409` cambio de estado no permitido.

```sh
# Login: guarda el token de cada usuario en una variable
login() {
  curl -s localhost:8080/api/login -H 'Content-Type: application/json' \
    -d "{\"usuario\":\"$1\",\"password\":\"$2\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/'
}
EMPLEADO=$(login empleado empleado123)
SUPERVISOR=$(login supervisor supervisor123)

# Transferencia de 60 EUR: supera el umbral (50 EUR) y genera una incidencia
curl -i localhost:8080/api/transferencias \
  -H "Authorization: Bearer $EMPLEADO" -H 'Content-Type: application/json' \
  -d '{"cuentaOrigenId":1,"cuentaDestinoId":2,"importe":60.00}'

curl -i -X POST localhost:8080/api/incidencias/1/revision -H "Authorization: Bearer $EMPLEADO"

# El empleado no puede resolver (403); el supervisor sí
curl -i localhost:8080/api/incidencias/1/resolucion \
  -H "Authorization: Bearer $SUPERVISOR" -H 'Content-Type: application/json' \
  -d '{"comentario":"Transferencia revisada; operación justificada."}'

curl localhost:8080/api/incidencias/1/historial -H "Authorization: Bearer $EMPLEADO"
```

El contenido del token no está cifrado, solo firmado: se puede leer (por ejemplo en
jwt.io), pero no modificar sin invalidar la firma.

## Pruebas

```sh
./mvnw test      # pruebas
./mvnw verify    # pruebas + empaquetado
```

- `ServicioTransferenciasTest`: reglas de negocio sin Spring.
- `BancoApiTest`: rutas, JSON y errores con MockMvc.
- `BancoPersistenciaTest`: SQL, rollback y transferencias concurrentes.
- `ReinicioPersistenciaTest`: los datos sobreviven a un reinicio.
- `ReglaImporteElevadoTest`: límites del umbral y moneda.
- `IncidenciasTest`: generación, transiciones de estado, historial, rollback y permisos por rol.
- `AuthTest`: login con JWT, contraseña incorrecta y acceso con y sin token.
- `AuthServidorTest`: 401 y 403 contra un servidor real (Tomcat), con token real.

Las pruebas usan bases aisladas y no modifican los datos de demostración.

## Integración continua

El [Jenkinsfile](Jenkinsfile) ejecuta `clean verify`, publica los informes JUnit y
archiva el JAR solo si todo pasa. Jenkins consulta GitHub cada cinco minutos.
Guía de instalación local con Docker: [docs/jenkins.md](docs/jenkins.md).

## Decisiones técnicas

- **JDBC con SQL explícito en vez de JPA**: control total sobre las consultas y
  los bloqueos, y SQL fácil de revisar.
- **Bloqueo de filas en orden de ID**: evita gastar dos veces el mismo saldo y
  previene interbloqueos entre transferencias cruzadas.
- **`BigDecimal` y `DECIMAL(19,2)`**: sin errores de redondeo en importes.
- **Detección y movimiento en la misma transacción**: no puede existir una
  transferencia marcada sin su incidencia, ni al revés.
- **Cambio de estado condicionado al estado previo**: dos revisores simultáneos no
  pueden aplicar la misma transición dos veces.
- **BCrypt para las contraseñas**: hash lento a propósito y con salt, para que un
  robo de la base no permita recuperar contraseñas por fuerza bruta.
- **JWT sin estado**: el servidor no guarda sesiones; verifica la firma de cada
  token sin consultar la base. La contraseña solo viaja una vez, en el login.
- **La interfaz guía, el backend protege**: el cliente deshabilita **Resolver** a
  quien no es supervisor, pero la defensa real es el `403` del backend.
- **Pruebas de seguridad contra un servidor real**: MockMvc y `@WithMockUser` no
  detectaron que un `403` llegaba como `401` (Spring reenvía los errores a `/error`
  sin el token); `AuthServidorTest` lo cubre.

## Limitaciones y próximos pasos

Es un proyecto de demostración. El umbral de 50 EUR es didáctico, no un criterio real.
Un token no se puede revocar antes de caducar (1 hora), y el cliente no lo renueva:
pasado ese tiempo hay que volver a iniciar sesión.

- [x] Autenticación y roles con Spring Security (BCrypt + JWT)
- [ ] Límite de intentos de login frente a fuerza bruta
- [ ] Contenedor Docker y despliegue en servidor Linux
- [ ] Despliegue automático desde Jenkins (CD)
- [ ] Importación y validación de ficheros de datos con incidencias de calidad
- [ ] Migraciones de esquema con Flyway y soporte para SQL Server / Oracle
- [ ] Paginación e idempotencia en las transferencias
