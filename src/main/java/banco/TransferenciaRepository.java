package banco;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.support.GeneratedKeyHolder;

@Repository
public class TransferenciaRepository {
    private final JdbcTemplate jdbc;

    public TransferenciaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long registrar(Cuenta origen, Cuenta destino, BigDecimal importe) {
        var clave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            var sentencia = conexion.prepareStatement("""
                INSERT INTO transferencias (cuenta_origen_id, cuenta_destino_id, importe, moneda)
                VALUES (?, ?, ?, ?)
                """, new String[]{"id"});
            sentencia.setLong(1, origen.getId());
            sentencia.setLong(2, destino.getId());
            sentencia.setBigDecimal(3, importe);
            sentencia.setString(4, origen.getMoneda().getCurrencyCode());
            return sentencia;
        }, clave);
        Number id = clave.getKey();
        if (id == null) {
            throw new IllegalStateException("No se obtuvo el ID de la transferencia.");
        }
        return id.longValue();
    }

    public List<Transferencia> listar() {
        return jdbc.query("SELECT * FROM transferencias ORDER BY id", (fila, numero) ->
                new Transferencia(fila.getLong("id"), fila.getLong("cuenta_origen_id"),
                        fila.getLong("cuenta_destino_id"), fila.getBigDecimal("importe"),
                        fila.getString("moneda"), fila.getObject("fecha", OffsetDateTime.class)));
    }
}
