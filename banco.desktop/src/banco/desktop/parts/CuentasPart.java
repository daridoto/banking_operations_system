package banco.desktop.parts;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletionException;
import banco.desktop.api.BancoApi;
import banco.desktop.api.CuentaVista;
import banco.desktop.api.ResumenVista;
import jakarta.annotation.PostConstruct;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

// Pestaña Cuentas: indicadores del día arriba, tabla de cuentas a la izquierda y
// formulario de transferencia a la derecha.
public class CuentasPart {
    private final BancoApi api = new BancoApi();
    private List<CuentaVista> cuentas = List.of();
    private Indicador transferenciasHoy;
    private Indicador importeHoy;
    private Indicador pendientes;
    private Indicador bloqueadas;
    private Table tabla;
    private Button actualizar;
    private Button transferir;
    private Combo origen;
    private Combo destino;
    private Text importe;
    private Text concepto;
    private Label estado;

    @PostConstruct
    public void createComposite(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        crearIndicadores(parent);
        estado = new Label(parent, SWT.WRAP);
        estado.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        SashForm paneles = new SashForm(parent, SWT.HORIZONTAL);
        paneles.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        crearTabla(paneles);
        crearFormulario(paneles);
        paneles.setWeights(65, 35);

        Display display = parent.getDisplay();
        actualizar.addListener(SWT.Selection, evento -> cargar(display));
        transferir.addListener(SWT.Selection, evento -> enviarTransferencia(display));
        parent.addDisposeListener(evento -> api.close());
        ocupado(false);
        cargar(display);
    }

    // Cuatro tarjetas que ocupan el ancho y el botón de actualizar a la derecha.
    private void crearIndicadores(Composite padre) {
        Composite fila = new Composite(padre, SWT.NONE);
        GridLayout layout = new GridLayout(5, false);
        layout.horizontalSpacing = 10;
        fila.setLayout(layout);
        fila.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        transferenciasHoy = indicador(fila, "Transferencias hoy");
        importeHoy = indicador(fila, "Importe movido hoy");
        pendientes = indicador(fila, "Incidencias pendientes");
        bloqueadas = indicador(fila, "Cuentas bloqueadas");
        actualizar = new Button(fila, SWT.PUSH);
        actualizar.setText("⟳  Actualizar");
        actualizar.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
    }

    private static Indicador indicador(Composite padre, String titulo) {
        Indicador tarjeta = new Indicador(padre, titulo);
        tarjeta.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return tarjeta;
    }

    private void crearTabla(Composite padre) {
        Group grupo = new Group(padre, SWT.NONE);
        grupo.setText("Cuentas");
        grupo.setLayout(new GridLayout(1, false));
        tabla = new Table(grupo, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        tabla.setHeaderVisible(true);
        tabla.setLinesVisible(true);
        tabla.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        // Los importes se alinean a la derecha para comparar cifras de un vistazo.
        String[] titulos = {"IBAN", "Titular", "Saldo", "Estado"};
        int[] anchos = {250, 200, 130, 110};
        int[] alineacion = {SWT.LEFT, SWT.LEFT, SWT.RIGHT, SWT.LEFT};
        for (int i = 0; i < titulos.length; i++) {
            TableColumn columna = new TableColumn(tabla, alineacion[i]);
            columna.setText(titulos[i]);
            columna.setWidth(anchos[i]);
        }
    }

    private void crearFormulario(Composite padre) {
        Group formulario = new Group(padre, SWT.NONE);
        formulario.setText("Nueva transferencia");
        formulario.setLayout(new GridLayout(2, false));
        new Label(formulario, SWT.NONE).setText("Cuenta de origen");
        origen = new Combo(formulario, SWT.READ_ONLY);
        origen.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        // El destino se puede elegir de la lista o escribir: en la vida real el IBAN
        // del beneficiario lo dicta el cliente. El backend comprueba que sea válido.
        new Label(formulario, SWT.NONE).setText("IBAN de destino");
        destino = new Combo(formulario, SWT.DROP_DOWN);
        destino.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(formulario, SWT.NONE).setText("Importe (€)");
        importe = new Text(formulario, SWT.BORDER | SWT.RIGHT);
        importe.setMessage("20,00");
        importe.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(formulario, SWT.NONE).setText("Concepto");
        concepto = new Text(formulario, SWT.BORDER);
        concepto.setMessage("Opcional, hasta 140 caracteres");
        concepto.setTextLimit(140);
        concepto.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        transferir = new Button(formulario, SWT.PUSH);
        transferir.setText("Transferir");
        GridData boton = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 2, 1);
        boton.verticalIndent = 8;
        transferir.setLayoutData(boton);
    }

    // Cuentas e indicadores se piden a la vez; cada respuesta pinta su parte.
    private void cargar(Display display) {
        ocupado(true);
        mensaje("Consultando cuentas…");
        actualizarResumen(display);
        api.listarCuentas().whenComplete((resultado, error) -> enPantalla(display, () -> {
            if (error == null) {
                cuentas = resultado;
                mostrarCuentas();
                origen.setItems(cuentas.stream().map(CuentaVista::etiqueta).toArray(String[]::new));
                destino.setItems(cuentas.stream()
                        .map(c -> c.ibanFormateado() + " (" + c.titular() + ")").toArray(String[]::new));
                if (!cuentas.isEmpty()) origen.select(0);
                if (cuentas.size() > 1) destino.select(1);
                mensaje(cuentas.isEmpty() ? "No hay cuentas." : "");
            } else {
                mensaje("No se pudieron actualizar las cuentas. Comprueba que el backend está arrancado.");
            }
            ocupado(false);
        }));
    }

    private void actualizarResumen(Display display) {
        api.consultarResumen().whenComplete((r, error) -> enPantalla(display, () -> {
            if (error != null) return; // Los indicadores se quedan como estaban.
            pintarResumen(r);
        }));
    }

    // Los indicadores de "atención" se ponen en color solo cuando hay algo que mirar.
    private void pintarResumen(ResumenVista r) {
        transferenciasHoy.setValor(Long.toString(r.transferenciasHoy()), null);
        importeHoy.setValor(Estilo.dinero(r.importeHoyEur(), "EUR"), null);
        pendientes.setValor(Long.toString(r.incidenciasPendientes()),
                r.incidenciasPendientes() > 0 ? Estilo.NARANJA : null);
        bloqueadas.setValor(Long.toString(r.cuentasBloqueadas()),
                r.cuentasBloqueadas() > 0 ? Estilo.ROJO : null);
    }

    // El clic recoge los valores y llama a la API; las reglas bancarias siguen en el backend.
    private void enviarTransferencia(Display display) {
        // Se descarta el titular entre paréntesis (si se eligió de la lista) y se quitan
        // los espacios: el IBAN viaja en formato electrónico.
        String ibanDestino = destino.getText().replaceAll("\\(.*$", "").replace(" ", "").toUpperCase();
        if (origen.getSelectionIndex() < 0 || ibanDestino.isEmpty()) {
            mensaje("Selecciona la cuenta de origen e indica el IBAN de destino.");
            return;
        }
        BigDecimal cantidad;
        try {
            String texto = importe.getText().strip();
            if (!texto.matches("[0-9]{1,15}([.,][0-9]{1,2})?")) throw new NumberFormatException();
            cantidad = new BigDecimal(texto.replace(',', '.'));
            if (cantidad.signum() <= 0) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            mensaje("Introduce un importe positivo, sin separadores de miles y con máximo dos decimales.");
            return;
        }
        String ibanOrigen = cuentas.get(origen.getSelectionIndex()).iban();
        if (ibanOrigen.equals(ibanDestino)) {
            mensaje("Selecciona cuentas distintas.");
            return;
        }
        ocupado(true); // Evita otro envío mientras esperamos la respuesta.
        mensaje("Enviando transferencia…");
        api.transferir(ibanOrigen, ibanDestino, cantidad, concepto.getText().strip())
                .whenComplete((resultado, error) -> enPantalla(display, () -> {
            if (error == null) {
                // POST devuelve las dos cuentas modificadas; conservamos las demás.
                cuentas = cuentas.stream().map(c -> resultado.stream()
                        .filter(nueva -> nueva.iban().equals(c.iban())).findFirst().orElse(c)).toList();
                mostrarCuentas();
                importe.setText("");
                concepto.setText("");
                mensaje("✔ Transferencia realizada. Saldos actualizados.");
                actualizarResumen(display);
            } else {
                Throwable causa = error;
                while (causa instanceof CompletionException && causa.getCause() != null) causa = causa.getCause();
                if (causa instanceof BancoApi.ErrorApi rechazo && rechazo.codigo() < 500) {
                    mensaje("Transferencia rechazada: " + rechazo.getMessage());
                } else {
                    mensaje("No se pudo confirmar el resultado. Consulta el historial de transferencias antes de repetir el envío.");
                }
            }
            ocupado(false);
        }));
    }

    private void mostrarCuentas() {
        tabla.removeAll();
        for (CuentaVista c : cuentas) {
            TableItem fila = new TableItem(tabla, SWT.NONE);
            fila.setText(new String[] {c.ibanFormateado(), c.titular(), Estilo.dinero(c.saldo(), c.moneda()),
                    "● " + c.estado()});
            fila.setForeground(3, Estilo.colorEstadoCuenta(c.estado()));
        }
    }

    private void ocupado(boolean valor) {
        actualizar.setEnabled(!valor);
        boolean disponible = !valor && cuentas.size() >= 2;
        origen.setEnabled(disponible);
        destino.setEnabled(disponible);
        importe.setEnabled(disponible);
        concepto.setEnabled(disponible);
        transferir.setEnabled(disponible);
    }

    private void mensaje(String texto) {
        estado.setText(texto);
        estado.getParent().layout();
    }

    // SWT solo permite modificar controles desde el hilo de la interfaz.
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
