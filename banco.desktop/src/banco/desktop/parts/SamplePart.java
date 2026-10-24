package banco.desktop.parts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class SamplePart {
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private Table tabla;
    private Button actualizar;
    private Label estado;
    private CompletableFuture<?> peticion;

    @PostConstruct
    public void createComposite(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        actualizar = new Button(parent, SWT.PUSH);
        actualizar.setText("Actualizar cuentas");
        estado = new Label(parent, SWT.WRAP);
        estado.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        estado.setText("Pulsa Actualizar cuentas para consultar el banco.");
        tabla = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        tabla.setHeaderVisible(true);
        tabla.setLinesVisible(true);
        tabla.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        String[] titulos = {"ID", "Titular", "Saldo", "Moneda"};
        int[] anchos = {70, 220, 130, 90};
        for (int i = 0; i < titulos.length; i++) {
            TableColumn columna = new TableColumn(tabla, SWT.NONE);
            columna.setText(titulos[i]);
            columna.setWidth(anchos[i]);
        }
        Display display = parent.getDisplay();
        actualizar.addListener(SWT.Selection, evento -> cargarCuentas(display));
        parent.addDisposeListener(evento -> {
            if (peticion != null) peticion.cancel(true);
            cliente.shutdownNow();
        });
    }

    private void cargarCuentas(Display display) {
        actualizar.setEnabled(false);
        estado.setText("Consultando cuentas…");
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/cuentas"))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json").GET().build();
        // La petición se hace en segundo plano para no bloquear la ventana.
        peticion = cliente.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                .handle((respuesta, error) -> {
                    List<String[]> filas = new ArrayList<>();
                    String mensaje;
                    if (error != null) {
                        mensaje = "No se pudo conectar. Arranca el backend en localhost:8080 y vuelve a intentarlo.";
                    } else if (respuesta.statusCode() != 200) {
                        mensaje = "El servidor devolvió HTTP " + respuesta.statusCode() + ".";
                    } else {
                        try {
                            filas = leerCuentas(respuesta.body());
                            mensaje = filas.isEmpty() ? "No hay cuentas." : "Cuentas actualizadas: " + filas.size();
                        } catch (RuntimeException ex) {
                            mensaje = "El servidor devolvió datos de cuentas no válidos.";
                        }
                    }
                    List<String[]> resultado = filas;
                    String texto = mensaje;
                    if (!display.isDisposed()) {
                        try {
                            display.asyncExec(() -> {
                                if (tabla.isDisposed()) return;
                                tabla.removeAll();
                                for (String[] fila : resultado) new TableItem(tabla, SWT.NONE).setText(fila);
                                estado.setText(texto);
                                estado.getParent().layout();
                                actualizar.setEnabled(true);
                            });
                        } catch (org.eclipse.swt.SWTException ex) {
                            if (ex.code != SWT.ERROR_DEVICE_DISPOSED) throw ex;
                        }
                    }
                    return null;
                });
    }

    static List<String[]> leerCuentas(String json) {
        List<String[]> filas = new ArrayList<>();
        for (var elemento : JsonParser.parseString(json).getAsJsonArray()) {
            var cuenta = elemento.getAsJsonObject();
            filas.add(new String[] {
                    Long.toString(cuenta.get("id").getAsLong()),
                    cuenta.get("titular").getAsString(),
                    cuenta.get("saldo").getAsBigDecimal().setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString(),
                    cuenta.get("moneda").getAsString()
            });
        }
        return filas;
    }

    @Focus
    public void setFocus() {
        if (tabla != null && !tabla.isDisposed()) tabla.setFocus();
    }
}
