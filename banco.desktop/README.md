# Cliente de escritorio RCP

Herramienta didáctica para consultar cuentas, generar transferencias de prueba y
gestionar incidencias. Consume la API del backend en `http://localhost:8080`.
No accede directamente a H2. Todavía no tiene usuarios ni permisos.

## Ejecutar desde Eclipse

1. Arranca el backend desde la raíz del repositorio con `./mvnw spring-boot:run`.
2. Abre el proyecto en Eclipse con PDE y Java 25. Tras cambios externos, usa Refresh.
3. Abre `banco.desktop.product` y pulsa **Launch an Eclipse application**.

## Probar el flujo

1. En **Cuentas**, pulsa **Actualizar cuentas**. Una transferencia válida de más
   de 50 EUR genera una incidencia; comprueba antes que el origen tenga saldo.
2. En **Incidencias**, pulsa **Actualizar incidencias** y selecciona la nueva fila.
3. **Iniciar revisión** solo se habilita para una incidencia pendiente.
4. **Resolver…** solo se habilita en revisión y exige comentario de 1 a 1000
   caracteres, no solo espacios. Cancelar el cuadro no envía ninguna petición.
5. Tras guardar, se actualizan el estado de la fila, el detalle y el historial.
   Una incidencia resuelta no permite ninguna de esas dos acciones.
6. El historial muestra los comentarios cuando existen; no añade una línea vacía
   para cambios sin comentario.

Las peticiones se realizan en segundo plano. Durante los cambios se bloquean los
controles para evitar envíos duplicados. Si el resultado es incierto, se consulta
de nuevo el estado, sin repetir automáticamente la escritura. Las validaciones y
transacciones definitivas siguen en el backend.

## Archivos principales

- `parts/SamplePart.java`: cuentas y formulario de transferencias.
- `parts/IncidenciasPart.java`: lista, detalle, historial y acciones de incidencias.
- `api/BancoApi.java`: llamadas HTTP y conversión de JSON.
- `api/*Vista.java`: datos recibidos por la interfaz.

Las rutas anteriores están bajo `src/banco/desktop`. `Application.e4xmi` define las
pestañas. El pipeline Maven/Jenkins de la raíz sigue comprobando solo el backend;
todavía no compila ni empaqueta este cliente RCP.
