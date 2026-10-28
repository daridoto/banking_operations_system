package banco.repository;

import banco.model.Cuenta;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CuentaRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Cuenta> convertirFila = (fila, numero) -> new Cuenta(
            fila.getLong("id"), fila.getString("iban"), fila.getString("titular"),
            fila.getBigDecimal("saldo"), fila.getString("moneda"), fila.getString("estado"));

    public CuentaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // El titular ya no es una columna: sale del cliente (razón social o nombre y apellidos).
    private static final String SELECT_CUENTA = """
            SELECT c.id, c.iban, COALESCE(cl.razon_social, cl.nombre || ' ' || cl.apellidos) AS titular,
                   c.saldo, c.moneda, c.estado
            FROM cuentas c JOIN clientes cl ON cl.id = c.cliente_id
            """;

    public List<Cuenta> listar() {
        return jdbc.query(SELECT_CUENTA + "ORDER BY c.id", convertirFila);
    }

    // FOR UPDATE bloquea la fila hasta que termine la transacción.
    public Cuenta buscarParaActualizar(String iban) {
        return jdbc.query(SELECT_CUENTA + "WHERE c.iban = ? FOR UPDATE", convertirFila, iban).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe ninguna cuenta con IBAN " + iban + "."));
    }

    public long contarPorEstado(String estado) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM cuentas WHERE estado = ?", Long.class, estado);
        return total == null ? 0 : total;
    }

    public void guardarSaldo(Cuenta cuenta) {
        int filas = jdbc.update("UPDATE cuentas SET saldo = ? WHERE id = ?",
                cuenta.getSaldo(), cuenta.getId());
        if (filas != 1) {
            throw new IllegalStateException("No se pudo actualizar la cuenta.");
        }
    }
}
