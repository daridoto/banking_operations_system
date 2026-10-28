package banco.repository;

import banco.model.Cuenta;
import banco.model.Transferencia;

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

    public long registrar(Cuenta origen, Cuenta destino, BigDecimal importe, String concepto, String creadaPor) {
        var clave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            var sentencia = conexion.prepareStatement("""
                INSERT INTO transferencias (cuenta_origen_id, cuenta_destino_id, importe, moneda, concepto, creada_por)
                VALUES (?, ?, ?, ?, ?, ?)
                """, new String[]{"id"});
            sentencia.setLong(1, origen.getId());
            sentencia.setLong(2, destino.getId());
            sentencia.setBigDecimal(3, importe);
            sentencia.setString(4, origen.getMoneda().getCurrencyCode());
            sentencia.setString(5, concepto);
            sentencia.setString(6, creadaPor);
            return sentencia;
        }, clave);
        Number id = clave.getKey();
        if (id == null) {
            throw new IllegalStateException("No se obtuvo el ID de la transferencia.");
        }
        return id.longValue();
    }

    public List<Transferencia> listar() {
        // Los IDs internos se traducen a IBAN uniendo dos veces con cuentas.
        return jdbc.query("""
                SELECT t.id, o.iban AS iban_origen, d.iban AS iban_destino, t.importe, t.moneda,
                       t.concepto, t.fecha, t.creada_por
                FROM transferencias t
                JOIN cuentas o ON o.id = t.cuenta_origen_id
                JOIN cuentas d ON d.id = t.cuenta_destino_id
                ORDER BY t.id
                """, (fila, numero) ->
                new Transferencia(fila.getLong("id"), fila.getString("iban_origen"),
                        fila.getString("iban_destino"), fila.getBigDecimal("importe"),
                        fila.getString("moneda"), fila.getString("concepto"),
                        fila.getObject("fecha", OffsetDateTime.class), fila.getString("creada_por")));
    }
}
