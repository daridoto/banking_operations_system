package banco;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CuentaRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Cuenta> convertirFila = (fila, numero) -> new Cuenta(
            fila.getLong("id"), fila.getString("titular"),
            fila.getBigDecimal("saldo"), fila.getString("moneda"));

    public CuentaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Cuenta> listar() {
        return jdbc.query("SELECT id, titular, saldo, moneda FROM cuentas ORDER BY id", convertirFila);
    }

    // FOR UPDATE bloquea la fila hasta que termine la transacción.
    public Cuenta buscarParaActualizar(long id) {
        return jdbc.query("SELECT id, titular, saldo, moneda FROM cuentas WHERE id = ? FOR UPDATE",
                        convertirFila, id).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe la cuenta " + id + "."));
    }

    public void guardarSaldo(Cuenta cuenta) {
        int filas = jdbc.update("UPDATE cuentas SET saldo = ? WHERE id = ?",
                cuenta.getSaldo(), cuenta.getId());
        if (filas != 1) {
            throw new IllegalStateException("No se pudo actualizar la cuenta.");
        }
    }
}
