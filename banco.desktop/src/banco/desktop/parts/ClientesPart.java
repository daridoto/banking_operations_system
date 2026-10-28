package banco.desktop.parts;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import banco.desktop.api.BancoApi;
import banco.desktop.api.ClienteVista;
import banco.desktop.api.CuentaVista;
import banco.desktop.api.FichaClienteVista;
import jakarta.annotation.PostConstruct;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

// Pestaña Clientes: buscador a la izquierda y ficha (vista 360) a la derecha.
public class ClientesPart {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final Locale ESPANA = Estilo.ESPANA;
    private static final Color VERDE = Estilo.VERDE;
    private static final Color ROJO = Estilo.ROJO;
    private static final Color GRIS = Estilo.GRIS;
    // Tiempo sin teclear antes de buscar, para no lanzar una petición por cada letra.
    private static final int ESPERA_BUSQUEDA_MS = 300;

    private final BancoApi api = new BancoApi();
    private List<ClienteVista> clientes = List.of();
    // Cada búsqueda o selección nueva invalida las respuestas pendientes de la anterior.
    private long busqueda;
    private long seleccion;
    private Runnable busquedaPendiente;

    private Text buscador;
    private Table tablaClientes;
    private Label recuento;
    private Composite derecha;
    private StackLayout pila;
    private Label aviso;
    private ScrolledComposite desplazable;
    private Composite ficha;
    private Avatar avatar;
    private Label nombre;
    private Label estadoCliente;
    private Label tipo;
    private Label documento;
    private Label alta;
    private Label numeroCuentas;
    private Label saldoTotal;
    private Table tablaCuentas;
    private Table tablaMovimientos;
    private Table tablaIncidencias;

    @PostConstruct
    public void createComposite(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        SashForm paneles = new SashForm(parent, SWT.HORIZONTAL);
        paneles.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        crearBuscador(paneles);
        crearFicha(paneles);
        paneles.setWeights(30, 70);
        parent.addDisposeListener(evento -> api.close());
        buscar(parent.getDisplay());
    }

    private void crearBuscador(Composite padre) {
        Group grupo = new Group(padre, SWT.NONE);
        grupo.setText("Buscar cliente");
        grupo.setLayout(new GridLayout(1, false));
        buscador = new Text(grupo, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        buscador.setMessage("Nombre, razón social o documento");
        buscador.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        tablaClientes = tabla(grupo, new String[] {"Nombre / Razón social", "Documento"}, new int[] {200, 100});
        recuento = new Label(grupo, SWT.NONE);
        recuento.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Display display = padre.getDisplay();
        buscador.addListener(SWT.Modify, evento -> programarBusqueda(display));
        tablaClientes.addListener(SWT.Selection, evento -> abrirFicha(display));
    }

    private void crearFicha(Composite padre) {
        // Dos capas apiladas: el aviso inicial y la ficha. Solo se ve una a la vez.
        derecha = new Composite(padre, SWT.NONE);
        pila = new StackLayout();
        derecha.setLayout(pila);
        aviso = new Label(derecha, SWT.CENTER | SWT.WRAP);
        aviso.setText("Selecciona un cliente para ver su ficha.");

        desplazable = new ScrolledComposite(derecha, SWT.V_SCROLL | SWT.H_SCROLL);
        desplazable.setExpandHorizontal(true);
        desplazable.setExpandVertical(true);
        ficha = new Composite(desplazable, SWT.NONE);
        ficha.setLayout(new GridLayout(1, false));
        desplazable.setContent(ficha);

        crearCabecera(ficha);
        tablaCuentas = tablaEnGrupo(ficha, "Cuentas",
                new String[] {"IBAN", "Saldo", "Estado", "Apertura"}, new int[] {260, 120, 100, 100});
        tablaMovimientos = tablaEnGrupo(ficha, "Últimos movimientos",
                new String[] {"Fecha", "Cuenta", "", "Contraparte", "Concepto", "Importe"},
                new int[] {95, 70, 30, 170, 170, 110});
        tablaIncidencias = tablaEnGrupo(ficha, "Incidencias",
                new String[] {"Nº", "Regla", "Importe", "Estado", "Resuelta por"}, new int[] {50, 160, 110, 120, 130});
        mostrar(aviso);
    }

    // Avatar a la izquierda; nombre, estado y datos clave a la derecha.
    private void crearCabecera(Composite padre) {
        Composite cabecera = new Composite(padre, SWT.NONE);
        cabecera.setLayout(new GridLayout(2, false));
        cabecera.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        avatar = new Avatar(cabecera);
        avatar.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        Composite datos = new Composite(cabecera, SWT.NONE);
        datos.setLayout(new GridLayout(4, false));
        datos.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        nombre = new Label(datos, SWT.NONE);
        nombre.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        Font grande = FontDescriptor.createFrom(nombre.getFont()).setStyle(SWT.BOLD).increaseHeight(6)
                .createFont(padre.getDisplay());
        nombre.setFont(grande);
        nombre.addDisposeListener(evento -> grande.dispose());
        estadoCliente = new Label(datos, SWT.NONE);
        estadoCliente.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        tipo = new Label(datos, SWT.NONE);
        tipo.setForeground(GRIS);
        tipo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));

        documento = dato(datos, "Documento");
        alta = dato(datos, "Cliente desde");
        numeroCuentas = dato(datos, "Nº cuentas");
        saldoTotal = dato(datos, "Saldo total");
    }

    private void programarBusqueda(Display display) {
        if (busquedaPendiente != null) display.timerExec(-1, busquedaPendiente);
        busquedaPendiente = () -> buscar(display);
        display.timerExec(ESPERA_BUSQUEDA_MS, busquedaPendiente);
    }

    private void buscar(Display display) {
        long esta = ++busqueda;
        recuento.setText("Buscando…");
        api.buscarClientes(buscador.getText().strip()).whenComplete((resultado, error) -> enPantalla(display, () -> {
            if (esta != busqueda) return; // Llegó tarde: ya hay una búsqueda más reciente.
            if (error != null) {
                recuento.setText("No se pudo consultar. Comprueba que el backend está arrancado.");
                return;
            }
            clientes = resultado;
            tablaClientes.removeAll();
            for (ClienteVista c : clientes) {
                new TableItem(tablaClientes, SWT.NONE).setText(new String[] {c.nombre(), c.documento()});
            }
            recuento.setText(clientes.size() == 1 ? "1 cliente" : clientes.size() + " clientes");
        }));
    }

    private void abrirFicha(Display display) {
        int indice = tablaClientes.getSelectionIndex();
        if (indice < 0) return;
        long esta = ++seleccion;
        aviso.setText("Cargando ficha…");
        mostrar(aviso);
        api.consultarCliente(clientes.get(indice).id()).whenComplete((resultado, error) -> enPantalla(display, () -> {
            if (esta != seleccion) return;
            if (error != null) {
                aviso.setText("No se pudo cargar la ficha del cliente.");
                return;
            }
            pintarFicha(resultado);
            mostrar(desplazable);
        }));
    }

    private void pintarFicha(FichaClienteVista f) {
        avatar.setEmpresa(f.esEmpresa());
        nombre.setText(f.nombre().toUpperCase(ESPANA));
        boolean activo = "ACTIVO".equals(f.estado());
        estadoCliente.setText("● " + f.estado());
        estadoCliente.setForeground(activo ? VERDE : GRIS);
        tipo.setText(f.esEmpresa() ? "Persona jurídica" : "Persona física");
        documento.setText(f.documento() + " (" + f.tipoDocumento() + ")");
        alta.setText(DIA.format(f.fechaAlta()));
        numeroCuentas.setText(Integer.toString(f.cuentas().size()));
        saldoTotal.setText(saldoTotal(f.cuentas()));

        tablaCuentas.removeAll();
        for (var c : f.cuentas()) {
            TableItem fila = new TableItem(tablaCuentas, SWT.NONE);
            fila.setText(new String[] {CuentaVista.formatearIban(c.iban()), dinero(c.saldo(), c.moneda()),
                    c.estado(), DIA.format(c.fechaApertura())});
            if (!"ACTIVA".equals(c.estado())) fila.setForeground(2, ROJO);
        }

        tablaMovimientos.removeAll();
        for (var m : f.movimientos()) {
            BigDecimal importe = m.esSalida() ? m.importe().negate() : m.importe();
            TableItem fila = new TableItem(tablaMovimientos, SWT.NONE);
            fila.setText(new String[] {DIA_HORA.format(m.fecha()), "…" + ultimos4(m.ibanPropio()),
                    m.esSalida() ? "→" : "←", m.contraparte(), m.concepto() == null ? "" : m.concepto(),
                    (m.esSalida() ? "" : "+") + dinero(importe, m.moneda())});
            fila.setForeground(5, m.esSalida() ? ROJO : VERDE);
        }
        if (f.movimientos().isEmpty()) filaVacia(tablaMovimientos, "Sin movimientos");

        tablaIncidencias.removeAll();
        for (var i : f.incidencias()) {
            boolean resuelta = "RESUELTA".equals(i.estado());
            TableItem fila = new TableItem(tablaIncidencias, SWT.NONE);
            fila.setText(new String[] {"#" + i.id(), i.regla(), dinero(i.importe(), i.moneda()),
                    (resuelta ? "✔ " : "") + i.estado(), i.resueltaPor() == null ? "" : i.resueltaPor()});
            fila.setForeground(3, resuelta ? VERDE : ROJO);
        }
        if (f.incidencias().isEmpty()) filaVacia(tablaIncidencias, "Sin incidencias");

        ficha.layout(true, true);
        desplazable.setMinSize(ficha.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    // Solo se suma si todas las cuentas están en la misma moneda: euros y dólares no se suman.
    private static String saldoTotal(List<FichaClienteVista.Cuenta> cuentas) {
        if (cuentas.isEmpty()) return "—";
        String moneda = cuentas.get(0).moneda();
        if (cuentas.stream().anyMatch(c -> !c.moneda().equals(moneda))) return "Varias monedas";
        return dinero(cuentas.stream().map(FichaClienteVista.Cuenta::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add), moneda);
    }

    private static String dinero(BigDecimal importe, String moneda) {
        return Estilo.dinero(importe, moneda);
    }

    private static String ultimos4(String iban) {
        return iban.substring(iban.length() - 4);
    }

    private static void filaVacia(Table tabla, String texto) {
        TableItem fila = new TableItem(tabla, SWT.NONE);
        fila.setText(0, texto);
        fila.setForeground(GRIS);
    }

    // Etiqueta gris + valor; devuelve el valor para rellenarlo después.
    private static Label dato(Composite padre, String titulo) {
        Label etiqueta = new Label(padre, SWT.NONE);
        etiqueta.setText(titulo);
        etiqueta.setForeground(GRIS);
        Label valor = new Label(padre, SWT.NONE);
        valor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return valor;
    }

    private static Table tablaEnGrupo(Composite padre, String titulo, String[] columnas, int[] anchos) {
        Group grupo = new Group(padre, SWT.NONE);
        grupo.setText(titulo);
        grupo.setLayout(new GridLayout(1, false));
        grupo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        Table tabla = tabla(grupo, columnas, anchos);
        GridData alto = new GridData(SWT.FILL, SWT.TOP, true, false);
        alto.heightHint = 110;
        tabla.setLayoutData(alto);
        return tabla;
    }

    private static Table tabla(Composite padre, String[] columnas, int[] anchos) {
        Table tabla = new Table(padre, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        tabla.setHeaderVisible(true);
        tabla.setLinesVisible(true);
        tabla.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        for (int i = 0; i < columnas.length; i++) {
            TableColumn columna = new TableColumn(tabla, SWT.NONE);
            columna.setText(columnas[i]);
            columna.setWidth(anchos[i]);
        }
        return tabla;
    }

    private void mostrar(Control capa) {
        pila.topControl = capa;
        derecha.layout();
    }

    // SWT solo permite modificar controles desde el hilo de la interfaz.
    private void enPantalla(Display display, Runnable accion) {
        if (display.isDisposed()) return;
        try {
            display.asyncExec(() -> { if (!tablaClientes.isDisposed()) accion.run(); });
        } catch (org.eclipse.swt.SWTException error) {
            if (error.code != SWT.ERROR_DEVICE_DISPOSED) throw error;
        }
    }

    @Focus
    public void setFocus() {
        if (buscador != null && !buscador.isDisposed()) buscador.setFocus();
    }
}
