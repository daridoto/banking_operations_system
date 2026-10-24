package banco.desktop.parts;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletionException;
import banco.desktop.api.BancoApi;
import banco.desktop.api.IncidenciaVista;
import jakarta.annotation.PostConstruct;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class IncidenciasPart {
    private final BancoApi api = new BancoApi();
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX");
    private Table tabla;
    private Text detalle;
    private Text historial;
    private Label estado;
    private Button actualizar;
    private Button revisar;
    private Button resolver;
    private IncidenciaVista actual;
    private boolean enviando;
    // Cada nueva selección invalida las respuestas pendientes de la anterior.
    private long seleccion;

    @PostConstruct
    public void createComposite(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        Composite acciones = new Composite(parent, SWT.NONE);
        acciones.setLayout(new GridLayout(3, false));
        acciones.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        actualizar = new Button(acciones, SWT.PUSH);
        actualizar.setText("Actualizar incidencias");
        revisar = new Button(acciones, SWT.PUSH);
        revisar.setText("Iniciar revisión");
        resolver = new Button(acciones, SWT.PUSH);
        resolver.setText("Resolver…");
        habilitarAcciones();
        estado = new Label(parent, SWT.WRAP);
        estado.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        estado.setText("Pulsa Actualizar incidencias para consultar el banco.");
        SashForm paneles = new SashForm(parent, SWT.VERTICAL);
        paneles.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabla = new Table(paneles, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        tabla.setHeaderVisible(true);
        tabla.setLinesVisible(true);
        tabla.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        String[] nombres = {"ID", "Transferencia", "Estado", "Prioridad", "Regla", "Fecha"};
        int[] anchos = {60, 100, 130, 90, 170, 230};
        for (int i = 0; i < nombres.length; i++) {
            TableColumn columna = new TableColumn(tabla, SWT.NONE);
            columna.setText(nombres[i]);
            columna.setWidth(anchos[i]);
        }
        Group grupo = new Group(paneles, SWT.NONE);
        grupo.setText("Detalle de la incidencia");
        grupo.setLayout(new GridLayout(1, false));
        grupo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        detalle = new Text(grupo, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData espacio = new GridData(SWT.FILL, SWT.FILL, true, true);
        detalle.setLayoutData(espacio);
        detalle.setText("Selecciona una incidencia de la tabla.");
        Group grupoHistorial = new Group(paneles, SWT.NONE);
        grupoHistorial.setText("Historial de cambios y comentarios");
        grupoHistorial.setLayout(new GridLayout(1, false));
        historial = new Text(grupoHistorial, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        historial.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        historial.setText("Selecciona una incidencia para consultar su historial.");
        paneles.setWeights(35, 30, 35);
        Display display = parent.getDisplay();
        actualizar.addListener(SWT.Selection, e -> cargarLista(display));
        tabla.addListener(SWT.Selection, e -> cargarDetalle(display));
        revisar.addListener(SWT.Selection, e -> cambiarEstado(display, false));
        resolver.addListener(SWT.Selection, e -> cambiarEstado(display, true));
        parent.addDisposeListener(e -> api.close());
    }

    private void cargarLista(Display display) {
        seleccion++;
        actual = null;
        habilitarAcciones();
        actualizar.setEnabled(false);
        tabla.setEnabled(false);
        tabla.removeAll();
        detalle.setText("");
        historial.setText("");
        mensaje("Consultando incidencias…");
        api.listarIncidencias().whenComplete((lista, error) -> enPantalla(display, () -> {
            if (error == null) {
                for (IncidenciaVista i : lista) {
                    TableItem fila = new TableItem(tabla, SWT.NONE);
                    fila.setData(i.id());
                    fila.setText(new String[] {Long.toString(i.id()), Long.toString(i.transferenciaId()),
                            i.estado(), i.prioridad(), i.regla(), FECHA.format(i.fecha())});
                }
                mensaje(lista.isEmpty() ? "No hay incidencias." : "Incidencias: " + lista.size());
                detalle.setText(lista.isEmpty() ? "No hay incidencias para consultar." : "Selecciona una incidencia de la tabla.");
            } else mensaje(describirError(error));
            actualizar.setEnabled(true);
            tabla.setEnabled(true);
        }));
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
            detalle.setText("Incidencia: " + i.id() + "\nTransferencia: " + i.transferenciaId()
                    + "\nEstado: " + i.estado() + "    Prioridad: " + i.prioridad()
                    + "\nRegla: " + i.regla() + "\nFecha: " + FECHA.format(i.fecha())
                    + "\nMotivo: " + i.motivo());
        }));
        api.consultarHistorialIncidencia(id).whenComplete((cambios, error) -> enPantalla(display, () -> {
            if (turno != seleccion) return;
            if (error != null) {
                historial.setText(describirError(error));
                return;
            }
            if (cambios.isEmpty()) {
                historial.setText("Esta incidencia todavía no tiene cambios de estado registrados.");
                return;
            }
            StringBuilder texto = new StringBuilder();
            for (var cambio : cambios) {
                if (!texto.isEmpty()) texto.append("\n\n");
                texto.append("Cambio ").append(cambio.id()).append(" — ").append(FECHA.format(cambio.fecha()))
                        .append("\n").append(cambio.estadoAnterior()).append(" → ").append(cambio.estadoNuevo());
                if (cambio.comentario() != null && !cambio.comentario().isBlank()) {
                    texto.append("\nComentario: ").append(cambio.comentario());
                }
            }
            historial.setText(texto.toString());
        }));
    }

    private void habilitarAcciones() {
        revisar.setEnabled(!enviando && actual != null && "PENDIENTE".equals(actual.estado()));
        resolver.setEnabled(!enviando && actual != null && "EN_REVISION".equals(actual.estado()));
    }

    private void actualizarFila(IncidenciaVista incidencia) {
        for (TableItem fila : tabla.getItems()) {
            if (Long.valueOf(incidencia.id()).equals(fila.getData())) {
                fila.setText(2, incidencia.estado());
                fila.setText(3, incidencia.prioridad());
            }
        }
    }

    private void cambiarEstado(Display display, boolean cerrar) {
        if (actual == null || enviando) return;
        long id = actual.id();
        String comentario = null;
        if (cerrar) {
            InputDialog dialogo = new InputDialog(tabla.getShell(), "Resolver incidencia " + id,
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
                mensaje(cerrar ? "Incidencia resuelta." : "Revisión iniciada.");
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
