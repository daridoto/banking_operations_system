package banco;

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
                    fila.getObject("fecha", OffsetDateTime.class));

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
        return jdbc.query("SELECT * FROM incidencias ORDER BY id", convertirFila);
    }

    public Incidencia buscar(long id) {
        return jdbc.query("SELECT * FROM incidencias WHERE id = ?", convertirFila, id)
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe la incidencia " + id + "."));
    }

    public boolean iniciarRevision(long id) {
        // La condición evita que dos peticiones confirmen el mismo cambio.
        return jdbc.update("UPDATE incidencias SET estado = 'EN_REVISION' WHERE id = ? AND estado = 'PENDIENTE'",
                id) == 1;
    }

    public List<CambioIncidencia> listarHistorial(long incidenciaId) {
        return jdbc.query("SELECT * FROM cambios_incidencia WHERE incidencia_id = ? ORDER BY fecha, id",
                (fila, numero) -> new CambioIncidencia(
                        fila.getLong("id"), fila.getLong("incidencia_id"),
                        fila.getString("estado_anterior"), fila.getString("estado_nuevo"),
                        fila.getObject("fecha", OffsetDateTime.class), fila.getString("comentario")),
                incidenciaId);
    }

    public void registrarInicioRevision(long id) {
        jdbc.update("""
                INSERT INTO cambios_incidencia (incidencia_id, estado_anterior, estado_nuevo)
                VALUES (?, 'PENDIENTE', 'EN_REVISION')
                """, id);
    }

    public boolean resolver(long id) {
        return jdbc.update("UPDATE incidencias SET estado = 'RESUELTA' WHERE id = ? AND estado = 'EN_REVISION'",
                id) == 1;
    }

    public void registrarResolucion(long id, String comentario) {
        jdbc.update("""
                INSERT INTO cambios_incidencia (incidencia_id, estado_anterior, estado_nuevo, comentario)
                VALUES (?, 'EN_REVISION', 'RESUELTA', ?)
                """, id, comentario);
    }
}
