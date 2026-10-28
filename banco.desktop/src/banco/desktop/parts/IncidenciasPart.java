package banco.desktop.parts;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionException;
import banco.desktop.api.BancoApi;
import banco.desktop.api.CambioIncidenciaVista;
import banco.desktop.api.IncidenciaVista;
import banco.desktop.api.Sesion;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

// Pestaña Incidencias: contadores por estado, cola de trabajo y, debajo, el detalle
// con el contexto de la transferencia y el historial de quién hizo qué.
public class IncidenciasPart {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String TITULO = "Incidencias";

    private final BancoApi api = new BancoApi();
    // La pestaña de la ventana, para mostrar en su título cuántas hay pendientes.
    @Inject private MPart pestana;
    private Indicador pendientes;
    private Indicador enRevision;
    private Indicador resueltas;
    private Table tabla;
    private Text detalle;
    private Text historial;
    private Label estado;
    private Button actualizar;
    private Button revisar;
    private Button resolver;
    private List<IncidenciaVista> incidencias = List.of();
    private IncidenciaVista actual;
    private boolean enviando;
    // Cada nueva selección invalida las respuestas pendientes de la anterior.
    private long seleccion;

    @PostConstruct
    public void createComposite(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        crearCabecera(parent);
        estado = new Label(parent, SWT.WRAP);
        estado.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        SashForm paneles = new SashForm(parent, SWT.VERTICAL);
        paneles.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        crearTabla(paneles);
        SashForm abajo = new SashForm(paneles, SWT.HORIZONTAL);
        detalle = textoEnGrupo(abajo, "Detalle", "Selecciona una incidencia de la tabla.");
        historial = textoEnGrupo(abajo, "Historial", "");
        abajo.setWeights(50, 50);
        paneles.setWeights(55, 45);

        Display display = parent.getDisplay();
        actualizar.addListener(SWT.Selection, e -> cargarLista(display));
        tabla.addListener(SWT.Selection, e -> cargarDetalle(display));
        revisar.addListener(SWT.Selection, e -> cambiarEstado(display, false));
        resolver.addListener(SWT.Selection, e -> cambiarEstado(display, true));
        parent.addDisposeListener(e -> api.close());
        cargarLista(display);
    }

    // Tres contadores y, a la derecha, las acciones.
    private void crearCabecera(Composite padre) {
        Composite fila = new Composite(padre, SWT.NONE);
        GridLayout layout = new GridLayout(4, false);
        layout.horizontalSpacing = 10;
        fila.setLayout(layout);
        fila.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        pendientes = indicador(fila, "Pendientes");
        enRevision = indicador(fila, "En revisión");
        resueltas = indicador(fila, "Resueltas");

        Composite acciones = new Composite(fila, SWT.NONE);
        acciones.setLayout(new GridLayout(3, false));
        acciones.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        actualizar = new Button(acciones, SWT.PUSH);
        actualizar.setText("⟳  Actualizar");
        revisar = new Button(acciones, SWT.PUSH);
        revisar.setText("Iniciar revisión");
        resolver = new Button(acciones, SWT.PUSH);
        resolver.setText("Resolver…");
        // Solo el supervisor resuelve; para los demás el botón ni se muestra.
        // La seguridad real está en el backend (403); esto solo adapta la vista.
        if (!"SUPERVISOR".equals(Sesion.rol())) {
            GridData oculto = new GridData();
            oculto.exclude = true;
            resolver.setLayoutData(oculto);
            resolver.setVisible(false);
        }
        habilitarAcciones();
    }

    private static Indicador indicador(Composite padre, String titulo) {
        Indicador tarjeta = new Indicador(padre, titulo);
        tarjeta.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return tarjeta;
    }

    private void crearTabla(Composite padre) {
        Group grupo = new Group(padre, SWT.NONE);
        grupo.setText("Cola de incidencias");
        grupo.setLayout(new GridLayout(1, false));
        tabla = new Table(grupo, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        tabla.setHeaderVisible(true);
        tabla.setLinesVisible(true);
        tabla.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        String[] nombres = {"Nº", "Estado", "Prioridad", "Regla", "Importe", "Ordenante → Beneficiario", "Fecha"};
        int[] anchos = {55, 135, 100, 150, 110, 300, 110};
        for (int i = 0; i < nombres.length; i++) {
            TableColumn columna = new TableColumn(tabla, i == 4 ? SWT.RIGHT : SWT.LEFT);
            columna.setText(nombres[i]);
            columna.setWidth(anchos[i]);
        }
    }

    private static Text textoEnGrupo(Composite padre, String titulo, String inicial) {
        Group grupo = new Group(padre, SWT.NONE);
        grupo.setText(titulo);
        grupo.setLayout(new GridLayout(1, false));
        Text texto = new Text(grupo, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        texto.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        texto.setText(inicial);
        return texto;
    }

    private void cargarLista(Display display) {
        seleccion++;
        actual = null;
        habilitarAcciones();
        actualizar.setEnabled(false);
        tabla.setEnabled(false);
        detalle.setText("");
        historial.setText("");
        mensaje("Consultando incidencias…");
        api.listarIncidencias().whenComplete((lista, error) -> enPantalla(display, () -> {
            if (error == null) {
                // Las más recientes arriba: es la cola de trabajo del día.
                incidencias = lista.stream().sorted(Comparator.comparingLong(IncidenciaVista::id).reversed()).toList();
                pintarTabla();
                mensaje(lista.isEmpty() ? "No hay incidencias." : "");
                detalle.setText(lista.isEmpty() ? "No hay incidencias para consultar." : "Selecciona una incidencia de la tabla.");
            } else mensaje(describirError(error));
            actualizar.setEnabled(true);
            tabla.setEnabled(true);
        }));
    }

    private void pintarTabla() {
        tabla.removeAll();
        for (IncidenciaVista i : incidencias) {
            TableItem fila = new TableItem(tabla, SWT.NONE);
            fila.setData(i.id());
            pintarFila(fila, i);
        }
        pintarContadores();
    }

    private void pintarFila(TableItem fila, IncidenciaVista i) {
        fila.setText(new String[] {"#" + i.id(), simboloEstado(i.estado()) + " " + nombreEstado(i.estado()),
                simboloPrioridad(i.prioridad()) + " " + i.prioridad(), i.regla(),
                Estilo.dinero(i.importe(), i.moneda()), i.ordenante() + "  →  " + i.beneficiario(),
                FECHA_CORTA.format(i.fecha())});
        fila.setForeground(1, colorEstado(i.estado()));
        fila.setForeground(2, colorPrioridad(i.prioridad()));
    }

    // Contadores de las tarjetas y del título de la pestaña ("Incidencias (3)").
    private void pintarContadores() {
        long p = contar("PENDIENTE");
        pendientes.setValor(Long.toString(p), p > 0 ? Estilo.NARANJA : null);
        enRevision.setValor(Long.toString(contar("EN_REVISION")), Estilo.AZUL);
        resueltas.setValor(Long.toString(contar("RESUELTA")), Estilo.VERDE);
        if (pestana != null) pestana.setLabel(p > 0 ? TITULO + " (" + p + ")" : TITULO);
    }

    private long contar(String estadoBuscado) {
        return incidencias.stream().filter(i -> estadoBuscado.equals(i.estado())).count();
    }

    private void cargarDetalle(Display display) {
        if (tabla.getSelectionCount() == 0) return;
        actual = null;
        habilitarAcciones();
        long id = (Long) tabla.getSelection()[0].getData();
        long turno = ++seleccion;
        detalle.setText("Consultando incidencia " + id + "…");
        historial.setText("Consultando historial…");
        api.consultarIncidencia(id).whenComplete((i, error) -> enPantalla(display, () -> {
            if (turno != seleccion) return;
            if (error != null) {
                detalle.setText(describirError(error));
                return;
            }
            actual = i;
            actualizarFila(i);
            habilitarAcciones();
            detalle.setText(describir(i));
        }));
        api.consultarHistorialIncidencia(id).whenComplete((cambios, error) -> enPantalla(display, () -> {
            if (turno != seleccion) return;
            if (error != null) {
                historial.setText(describirError(error));
                return;
            }
            historial.setText(cambios.isEmpty()
                    ? "Todavía no hay cambios: la incidencia está pendiente de revisión."
                    : describir(cambios));
        }));
    }

    // Lo que el revisor necesita saber, en el orden en que lo leería.
    private static String describir(IncidenciaVista i) {
        StringBuilder texto = new StringBuilder()
                .append("Incidencia #").append(i.id()).append("  ·  ").append(nombreEstado(i.estado()))
                .append("  ·  prioridad ").append(i.prioridad().toLowerCase())
                .append("\n\nTransferencia #").append(i.transferenciaId()).append("  ·  ")
                .append(Estilo.dinero(i.importe(), i.moneda()))
                .append("\nDe:  ").append(i.ordenante())
                .append("\nA:    ").append(i.beneficiario());
        if (i.concepto() != null) texto.append("\nConcepto:  ").append(i.concepto());
        texto.append("\n\nRegla:  ").append(i.regla())
                .append("\nMotivo:  ").append(i.motivo())
                .append("\nDetectada:  ").append(FECHA.format(i.fecha()));
        return texto.toString();
    }

    // Cada cambio con su fecha, la transición y quién lo hizo (nombre real si se conoce).
    private static String describir(List<CambioIncidenciaVista> cambios) {
        StringBuilder texto = new StringBuilder();
        for (var cambio : cambios) {
            if (!texto.isEmpty()) texto.append("\n\n");
            texto.append(FECHA.format(cambio.fecha())).append("   ")
                    .append(nombreEstado(cambio.estadoAnterior())).append("  →  ")
                    .append(nombreEstado(cambio.estadoNuevo()));
            String quien = cambio.nombreUsuario() != null ? cambio.nombreUsuario() : cambio.usuario();
            if (quien != null) texto.append("\n                       por ").append(quien);
            if (cambio.comentario() != null && !cambio.comentario().isBlank()) {
                texto.append("\n                       “").append(cambio.comentario()).append("”");
            }
        }
        return texto.toString();
    }

    private static String nombreEstado(String estado) {
        return switch (estado) {
            case "PENDIENTE" -> "Pendiente";
            case "EN_REVISION" -> "En revisión";
            case "RESUELTA" -> "Resuelta";
            default -> estado;
        };
    }

    private static String simboloEstado(String estado) {
        return "RESUELTA".equals(estado) ? "✔" : "●";
    }

    private static Color colorEstado(String estado) {
        return switch (estado) {
            case "PENDIENTE" -> Estilo.NARANJA;
            case "EN_REVISION" -> Estilo.AZUL;
            case "RESUELTA" -> Estilo.VERDE;
            default -> Estilo.GRIS;
        };
    }

    private static String simboloPrioridad(String prioridad) {
        return switch (prioridad) {
            case "ALTA" -> "▲";
            case "BAJA" -> "▼";
            default -> "■";
        };
    }

    private static Color colorPrioridad(String prioridad) {
        return switch (prioridad) {
            case "ALTA" -> Estilo.ROJO;
            case "MEDIA" -> Estilo.NARANJA;
            default -> Estilo.GRIS;
        };
    }

    private void habilitarAcciones() {
        revisar.setEnabled(!enviando && actual != null && "PENDIENTE".equals(actual.estado()));
        resolver.setEnabled(!enviando && actual != null && "EN_REVISION".equals(actual.estado()));
    }

    // Sustituye la incidencia en la lista y repinta su fila y los contadores.
    private void actualizarFila(IncidenciaVista incidencia) {
        incidencias = incidencias.stream().map(i -> i.id() == incidencia.id() ? incidencia : i).toList();
        for (TableItem fila : tabla.getItems()) {
            if (Long.valueOf(incidencia.id()).equals(fila.getData())) pintarFila(fila, incidencia);
        }
        pintarContadores();
    }

    private void cambiarEstado(Display display, boolean cerrar) {
        if (actual == null || enviando) return;
        long id = actual.id();
        String comentario = null;
        if (cerrar) {
            InputDialog dialogo = new InputDialog(tabla.getShell(), "Resolver incidencia #" + id,
                    "Escribe el motivo de resolución (máximo 1000 caracteres):", "",
                    texto -> texto == null || texto.isBlank() || texto.length() > 1000
                            ? "El comentario es obligatorio y admite hasta 1000 caracteres." : null);
            if (dialogo.open() != Window.OK) return;
            comentario = dialogo.getValue().strip();
        }
        // Desactivamos los controles para evitar envíos duplicados y cambios de selección.
        seleccion++;
        enviando = true;
        actualizar.setEnabled(false);
        tabla.setEnabled(false);
        habilitarAcciones();
        mensaje(cerrar ? "Guardando resolución…" : "Iniciando revisión…");
        var peticion = cerrar ? api.resolverIncidencia(id, comentario) : api.iniciarRevision(id);
        peticion.whenComplete((incidencia, error) -> enPantalla(display, () -> {
            enviando = false;
            actualizar.setEnabled(true);
            tabla.setEnabled(true);
            if (error == null) {
                actualizarFila(incidencia);
                mensaje(cerrar ? "✔ Incidencia resuelta." : "✔ Revisión iniciada.");
            } else {
                Throwable causa = error;
                while (causa instanceof CompletionException && causa.getCause() != null) causa = causa.getCause();
                mensaje(causa instanceof BancoApi.ErrorApi respuesta && respuesta.codigo() < 500
                        ? respuesta.getMessage()
                        : "No se pudo confirmar el cambio. Comprueba el estado y el historial antes de repetirlo.");
            }
            // Volvemos a consultar también ante un conflicto o resultado incierto, sin repetir el POST.
            cargarDetalle(display);
        }));
    }

    private String describirError(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
        if (error instanceof BancoApi.ErrorApi respuesta) return respuesta.getMessage();
        return "No se pudieron obtener los datos. Comprueba el backend y vuelve a intentarlo.";
    }

    private void mensaje(String texto) {
        estado.setText(texto);
        estado.getParent().layout();
    }

    private void enPantalla(Display display, Runnable accion) {
        if (display.isDisposed()) return;
        try {
            display.asyncExec(() -> { if (!tabla.isDisposed()) accion.run(); });
        } catch (org.eclipse.swt.SWTException error) {
            if (error.code != SWT.ERROR_DEVICE_DISPOSED) throw error;
        }
    }

    @Focus
    public void setFocus() {
        if (tabla != null && !tabla.isDisposed()) tabla.setFocus();
    }
}
