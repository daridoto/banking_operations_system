package banco.repository;

import banco.model.Incidencia;
import banco.model.CambioIncidencia;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class IncidenciaRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Incidencia> convertirFila = (fila, numero) ->
            new Incidencia(fila.getLong("id"), fila.getLong("transferencia_id"),
                    fila.getString("regla"), fila.getString("motivo"),
                    fila.getString("prioridad"), fila.getString("estado"),
                    fila.getObject("fecha", OffsetDateTime.class),
                    fila.getBigDecimal("importe"), fila.getString("moneda"), fila.getString("ordenante"),
                    fila.getString("beneficiario"), fila.getString("concepto"));

    // La incidencia unida a su transferencia y a los clientes de las dos cuentas.
    private static final String SELECT_INCIDENCIA = """
            SELECT i.*, t.importe, t.moneda, t.concepto,
                   COALESCE(co.razon_social, co.nombre || ' ' || co.apellidos) AS ordenante,
                   COALESCE(cd.razon_social, cd.nombre || ' ' || cd.apellidos) AS beneficiario
            FROM incidencias i
            JOIN transferencias t ON t.id = i.transferencia_id
            JOIN cuentas o ON o.id = t.cuenta_origen_id
            JOIN clientes co ON co.id = o.cliente_id
            JOIN cuentas d ON d.id = t.cuenta_destino_id
            JOIN clientes cd ON cd.id = d.cliente_id
            """;

    public IncidenciaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(long transferenciaId, String regla, String motivo) {
        jdbc.update("""
                INSERT INTO incidencias (transferencia_id, regla, motivo, prioridad, estado)
                VALUES (?, ?, ?, 'MEDIA', 'PENDIENTE')
                """, transferenciaId, regla, motivo);
    }

    public List<Incidencia> listar() {
        return jdbc.query(SELECT_INCIDENCIA + "ORDER BY i.id", convertirFila);
    }

    public Incidencia buscar(long id) {
        return jdbc.query(SELECT_INCIDENCIA + "WHERE i.id = ?", convertirFila, id)
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe la incidencia " + id + "."));
    }

    public boolean iniciarRevision(long id) {
        // La condición evita que dos peticiones confirmen el mismo cambio.
        return jdbc.update("UPDATE incidencias SET estado = 'EN_REVISION' WHERE id = ? AND estado = 'PENDIENTE'",
                id) == 1;
    }

    public List<CambioIncidencia> listarHistorial(long incidenciaId) {
        // LEFT JOIN: los cambios anteriores a la auditoría (V2) no tienen usuario.
        return jdbc.query("""
                SELECT c.*, u.nombre_completo AS nombre_usuario
                FROM cambios_incidencia c LEFT JOIN usuarios u ON u.username = c.usuario
                WHERE c.incidencia_id = ? ORDER BY c.fecha, c.id
                """,
                (fila, numero) -> new CambioIncidencia(
                        fila.getLong("id"), fila.getLong("incidencia_id"),
                        fila.getString("estado_anterior"), fila.getString("estado_nuevo"),
                        fila.getObject("fecha", OffsetDateTime.class), fila.getString("comentario"),
                        fila.getString("usuario"), fila.getString("nombre_usuario")),
                incidenciaId);
    }

    public long contarPorEstado(String estado) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM incidencias WHERE estado = ?", Long.class, estado);
        return total == null ? 0 : total;
    }

    public void registrarInicioRevision(long id, String usuario) {
        jdbc.update("""
                INSERT INTO cambios_incidencia (incidencia_id, estado_anterior, estado_nuevo, usuario)
                VALUES (?, 'PENDIENTE', 'EN_REVISION', ?)
                """, id, usuario);
    }

    public boolean resolver(long id) {
        return jdbc.update("UPDATE incidencias SET estado = 'RESUELTA' WHERE id = ? AND estado = 'EN_REVISION'",
                id) == 1;
    }

    public void registrarResolucion(long id, String comentario, String usuario) {
        jdbc.update("""
                INSERT INTO cambios_incidencia (incidencia_id, estado_anterior, estado_nuevo, comentario, usuario)
                VALUES (?, 'EN_REVISION', 'RESUELTA', ?, ?)
                """, id, comentario, usuario);
    }
}
