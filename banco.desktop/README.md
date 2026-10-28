# Cliente de escritorio RCP

Herramienta para empleados del banco: cuentas, transferencias por IBAN, gestión
de incidencias y ficha de cliente. Consume la API del backend en `http://localhost:8080`.
No accede directamente a H2.

## Inicio de sesión

Al arrancar se muestra un diálogo de login; sin credenciales válidas no se abre la
aplicación. Usuarios de demostración: `esanchez`/`empleado123` (empleada) y
`jmoreno`/`supervisor123` (supervisor).

1. El cliente envía usuario y contraseña una sola vez a `POST /api/login` y recibe
   un token JWT. La contraseña no se guarda.
2. `Sesion` guarda el token en memoria y `BancoApi` lo añade como
   `Authorization: Bearer <token>` a todas las peticiones.
3. Con el token consulta `GET /api/me` para conocer el rol. Si no es `SUPERVISOR`,
   **Resolver…** queda deshabilitado; el backend lo rechaza igualmente con `403`.

El token caduca en una hora y el cliente no lo renueva: pasado ese tiempo hay que
reiniciar el cliente.

## Ejecutar desde Eclipse

1. Arranca el backend desde la raíz del repositorio con `./mvnw spring-boot:run`.
2. Abre el proyecto en Eclipse con PDE y Java 25. Tras cambios externos, usa Refresh.
3. Abre `banco.desktop.product` y pulsa **Launch an Eclipse application**.

## Probar el flujo

1. En **Cuentas**, pulsa **Actualizar cuentas**. Elige la cuenta de origen e indica
   el IBAN de destino (de la lista, con su titular, o escrito a mano) y un concepto
   opcional. Una transferencia de más de 50 EUR genera una incidencia.
2. En **Incidencias**, pulsa **Actualizar incidencias** y selecciona la nueva fila.
3. **Iniciar revisión** solo se habilita para una incidencia pendiente.
4. **Resolver…** solo se habilita en revisión y para un supervisor, y exige comentario de 1 a 1000
   caracteres, no solo espacios. Cancelar el cuadro no envía ninguna petición.
5. Tras guardar, se actualizan el estado de la fila, el detalle y el historial.
   Una incidencia resuelta no permite ninguna de esas dos acciones.
6. El historial muestra los comentarios cuando existen y qué empleado hizo cada
   cambio ("· por supervisor").
7. En **Clientes**, busca por nombre, razón social o documento (la búsqueda se lanza
   al dejar de escribir). Al seleccionar un cliente se abre su ficha: avatar de
   persona o empresa, documento, estado, saldo total, cuentas, últimos movimientos
   (salidas en rojo, entradas en verde) e incidencias.

Las peticiones se realizan en segundo plano. Durante los cambios se bloquean los
controles para evitar envíos duplicados. Si el resultado es incierto, se consulta
de nuevo el estado, sin repetir automáticamente la escritura. Las validaciones y
transacciones definitivas siguen en el backend.

## Archivos principales

- `parts/CuentasPart.java`: indicadores del día, cuentas y formulario de transferencias.
- `parts/Cabecera.java`: barra superior con la marca y el empleado conectado.
- `parts/Indicador.java` y `parts/Estilo.java`: tarjeta de indicador, colores y formato de importes.
- `parts/IncidenciasPart.java`: lista, detalle, historial y acciones de incidencias.
- `parts/ClientesPart.java`: buscador y ficha del cliente.
- `parts/Avatar.java`: silueta de persona o edificio dibujada con código (GC).
- `LifeCycle.java` y `LoginDialog.java`: login al arrancar, antes de abrir la ventana.
- `api/Sesion.java`: usuario, token y rol en memoria.
- `api/BancoApi.java`: llamadas HTTP, cabecera `Bearer` y conversión de JSON.
- `api/*Vista.java`: datos recibidos por la interfaz.

Las rutas anteriores están bajo `src/banco/desktop`. El login se registra con
`-lifeCycleURI` en los argumentos de `banco.desktop.product`. `Application.e4xmi` define las
pestañas. El pipeline Maven/Jenkins de la raíz sigue comprobando solo el backend;
todavía no compila ni empaqueta este cliente RCP.
