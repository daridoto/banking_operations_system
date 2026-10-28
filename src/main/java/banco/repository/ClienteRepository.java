package banco.repository;

import banco.dto.ClienteResumen;
import banco.dto.FichaCliente;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteRepository {
    // Nombre para mostrar: razón social si es empresa, nombre y apellidos si es persona.
    private static final String NOMBRE = "COALESCE(cl.razon_social, cl.nombre || ' ' || cl.apellidos)";
    private static final int MAX_MOVIMIENTOS = 10;

    private final JdbcTemplate jdbc;

    public ClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Busca por nombre, apellidos, razón social o documento, sin distinguir mayúsculas.
    // Los comodines de LIKE (% y _) que escriba el usuario se tratan como texto normal.
    public List<ClienteResumen> buscar(String texto) {
        String patron = "%" + texto.toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return jdbc.query("SELECT cl.id, cl.tipo, " + NOMBRE + " AS nombre, cl.documento, cl.estado "
                        + "FROM clientes cl "
                        + "WHERE LOWER(" + NOMBRE + ") LIKE ? OR LOWER(cl.documento) LIKE ? "
                        + "ORDER BY nombre",
                (fila, n) -> new ClienteResumen(fila.getLong("id"), fila.getString("tipo"),
                        fila.getString("nombre"), fila.getString("documento"), fila.getString("estado")),
                patron, patron);
    }

    public FichaCliente ficha(long id) {
        return jdbc.query("SELECT cl.id, cl.tipo, " + NOMBRE + " AS nombre, cl.documento, cl.estado, "
                        + "cl.fecha_alta FROM clientes cl WHERE cl.id = ?",
                (fila, n) -> {
                    String tipo = fila.getString("tipo");
                    String documento = fila.getString("documento");
                    return new FichaCliente(fila.getLong("id"), tipo, fila.getString("nombre"), documento,
                            tipoDocumento(tipo, documento), fila.getString("estado"),
                            fila.getObject("fecha_alta", LocalDate.class),
                            cuentas(id), movimientos(id), incidencias(id));
                }, id).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe el cliente " + id + "."));
    }

    private List<FichaCliente.CuentaCliente> cuentas(long clienteId) {
        return jdbc.query("""
                SELECT iban, saldo, moneda, estado, fecha_apertura
                FROM cuentas WHERE cliente_id = ? ORDER BY fecha_apertura, id
                """, (fila, n) -> new FichaCliente.CuentaCliente(fila.getString("iban"),
                        fila.getBigDecimal("saldo"), fila.getString("moneda"), fila.getString("estado"),
                        fila.getObject("fecha_apertura", LocalDate.class)),
                clienteId);
    }

    // "propia" es la cuenta del cliente que participa; "otra", la del otro lado.
    // Si el cliente transfiere entre sus propias cuentas, salen dos filas: salida y entrada.
    private List<FichaCliente.Movimiento> movimientos(long clienteId) {
        return jdbc.query("""
                SELECT t.id, t.fecha, t.importe, t.moneda, t.concepto,
                       propia.iban AS iban_propio, otra.iban AS iban_contraparte,
                       COALESCE(cl.razon_social, cl.nombre || ' ' || cl.apellidos) AS contraparte,
                       CASE WHEN t.cuenta_origen_id = propia.id THEN 'SALIDA' ELSE 'ENTRADA' END AS sentido
                FROM transferencias t
                JOIN cuentas propia ON propia.id IN (t.cuenta_origen_id, t.cuenta_destino_id)
                JOIN cuentas otra ON otra.id = CASE WHEN t.cuenta_origen_id = propia.id
                                                    THEN t.cuenta_destino_id ELSE t.cuenta_origen_id END
                JOIN clientes cl ON cl.id = otra.cliente_id
                WHERE propia.cliente_id = ?
                ORDER BY t.fecha DESC, t.id DESC
                LIMIT ?
                """, (fila, n) -> new FichaCliente.Movimiento(fila.getLong("id"),
                        fila.getObject("fecha", OffsetDateTime.class), fila.getString("iban_propio"),
                        fila.getString("sentido"), fila.getString("iban_contraparte"),
                        fila.getString("contraparte"), fila.getString("concepto"),
                        fila.getBigDecimal("importe"), fila.getString("moneda")),
                clienteId, MAX_MOVIMIENTOS);
    }

    // Incidencias de transferencias en las que participa alguna cuenta del cliente.
    // EXISTS evita duplicados cuando las dos cuentas de la transferencia son suyas.
    private List<FichaCliente.IncidenciaCliente> incidencias(long clienteId) {
        return jdbc.query("""
                SELECT i.id, i.regla, t.importe, t.moneda, i.estado, i.fecha,
                       (SELECT c.usuario FROM cambios_incidencia c
                        WHERE c.incidencia_id = i.id AND c.estado_nuevo = 'RESUELTA') AS resuelta_por
                FROM incidencias i
                JOIN transferencias t ON t.id = i.transferencia_id
                WHERE EXISTS (SELECT 1 FROM cuentas cu
                              WHERE cu.cliente_id = ? AND cu.id IN (t.cuenta_origen_id, t.cuenta_destino_id))
                ORDER BY i.id DESC
                """, (fila, n) -> new FichaCliente.IncidenciaCliente(fila.getLong("id"),
                        fila.getString("regla"), fila.getBigDecimal("importe"), fila.getString("moneda"),
                        fila.getString("estado"), fila.getObject("fecha", OffsetDateTime.class),
                        fila.getString("resuelta_por")),
                clienteId);
    }

    // Las personas tienen DNI, o NIE si son extranjeras (empieza por X, Y o Z);
    // las empresas, CIF.
    private static String tipoDocumento(String tipo, String documento) {
        if ("JURIDICA".equals(tipo)) {
            return "CIF";
        }
        return "XYZ".indexOf(documento.charAt(0)) >= 0 ? "NIE" : "DNI";
    }
}
