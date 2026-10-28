package banco;

import static banco.DatosPrueba.IBAN_ANA;
import static banco.DatosPrueba.IBAN_LUIS;

import banco.service.BancoService;
import banco.dto.SolicitudTransferencia;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BancoPersistenciaTest {
    @Autowired private BancoService banco;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void guardaSaldosYRegistroEnSql() {
        banco.transferir(new SolicitudTransferencia(IBAN_ANA, IBAN_LUIS, new BigDecimal("20.00"), null), "empleado");

        assertEquals(new BigDecimal("80.00"), saldo(1));
        assertEquals(new BigDecimal("70.00"), saldo(2));
        var historial = banco.listarTransferencias();
        assertEquals(1, historial.size());
        assertEquals(IBAN_ANA, historial.get(0).ibanOrigen());
        assertEquals(IBAN_LUIS, historial.get(0).ibanDestino());
        assertEquals(new BigDecimal("20.00"), historial.get(0).importe());
        assertEquals("EUR", historial.get(0).moneda());
        assertNotNull(historial.get(0).fecha());
    }

    @Test
    void revierteAmbosSaldosSiFallaElRegistroFinal() {
        // Provocar un fallo SQL DESPUÉS de las dos actualizaciones de saldo.
        jdbc.execute("ALTER TABLE transferencias ADD CONSTRAINT fallo_prueba CHECK (importe <> 20)");

        assertThrows(DataAccessException.class, () -> banco.transferir(
                new SolicitudTransferencia(IBAN_ANA, IBAN_LUIS, new BigDecimal("20.00"), null), "empleado"));

        assertEquals(new BigDecimal("100.00"), saldo(1));
        assertEquals(new BigDecimal("50.00"), saldo(2));
        assertTrue(banco.listarTransferencias().isEmpty());
    }

    @Test
    void rechazaFraccionesDeCentimoSinRedondearNiRegistrar() {
        assertThrows(IllegalArgumentException.class, () -> banco.transferir(
                new SolicitudTransferencia(IBAN_ANA, IBAN_LUIS, new BigDecimal("0.001"), null), "empleado"));
        assertEquals(new BigDecimal("100.00"), saldo(1));
        assertEquals(new BigDecimal("50.00"), saldo(2));
        assertTrue(banco.listarTransferencias().isEmpty());
    }

    @Test
    void peticionesSimultaneasNoGastanElMismoSaldoDosVeces() throws Exception {
        var ejecutor = Executors.newFixedThreadPool(4);
        try {
            var peticiones = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 10; i++) {
                peticiones.add(() -> {
                    try {
                        banco.transferir(new SolicitudTransferencia(IBAN_ANA, IBAN_LUIS, new BigDecimal("20.00"), null), "empleado");
                        return true;
                    } catch (IllegalArgumentException sinSaldo) {
                        assertEquals("Saldo insuficiente.", sinSaldo.getMessage());
                        return false;
                    }
                });
            }
            int realizadas = 0;
            for (var resultado : ejecutor.invokeAll(peticiones, 15, TimeUnit.SECONDS)) {
                if (resultado.get()) realizadas++;
            }
            assertEquals(5, realizadas);
            assertEquals(new BigDecimal("0.00"), saldo(1));
            assertEquals(new BigDecimal("150.00"), saldo(2));
            assertEquals(5, banco.listarTransferencias().size());
        } finally {
            ejecutor.shutdownNow();
        }
    }

    private BigDecimal saldo(long id) {
        return jdbc.queryForObject("SELECT saldo FROM cuentas WHERE id = ?", BigDecimal.class, id);
    }
}
