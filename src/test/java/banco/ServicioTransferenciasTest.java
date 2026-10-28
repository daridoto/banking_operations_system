package banco;

import static banco.DatosPrueba.IBAN_ANA;
import static banco.DatosPrueba.IBAN_LUIS;

import banco.service.ServicioTransferencias;
import banco.model.Cuenta;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioTransferenciasTest {
    private final ServicioTransferencias servicio = new ServicioTransferencias();
    private final Cuenta ana = new Cuenta(1L, IBAN_ANA, "Ana", new BigDecimal("100.00"), "EUR", "ACTIVA");
    private final Cuenta luis = new Cuenta(2L, IBAN_LUIS, "Luis", new BigDecimal("50.00"), "EUR", "ACTIVA");

    @Test
    void transfiereDineroEntreDosCuentas() {
        // Preparar: las cuentas de arriba comienzan con 100 y 50 euros.
        // Actuar: ejecutamos la operación que queremos probar.
        servicio.transferir(ana, luis, new BigDecimal("20.00"));

        // Comprobar: el resultado debe coincidir con lo esperado.
        assertEquals(new BigDecimal("80.00"), ana.getSaldo());
        assertEquals(new BigDecimal("70.00"), luis.getSaldo());
    }

    @Test
    void permiteTransferirTodoElSaldo() {
        servicio.transferir(ana, luis, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("0.00"), ana.getSaldo());
        assertEquals(new BigDecimal("150.00"), luis.getSaldo());
    }

    @Test
    void rechazaSaldoInsuficienteSinCambiarSaldos() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.transferir(ana, luis, new BigDecimal("100.01")));

        comprobarSaldosIniciales();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-20.00"})
    void rechazaImportesNoPositivosSinCambiarSaldos(String importe) {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.transferir(ana, luis, new BigDecimal(importe)));

        comprobarSaldosIniciales();
    }

    @Test
    void rechazaTransferirALaMismaCuenta() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.transferir(ana, ana, new BigDecimal("20.00")));

        comprobarSaldosIniciales();
    }

    @Test
    void rechazaMonedasDistintasSinCambiarSaldos() {
        Cuenta dolares = new Cuenta(3L, "DE89370400440532013000", "Eva", new BigDecimal("50.00"), "USD", "ACTIVA");

        assertThrows(IllegalArgumentException.class,
                () -> servicio.transferir(ana, dolares, new BigDecimal("20.00")));

        assertEquals(new BigDecimal("100.00"), ana.getSaldo());
        assertEquals(new BigDecimal("50.00"), dolares.getSaldo());
    }

    @Test
    void rechazaImporteNuloSinCambiarSaldos() {
        assertThrows(NullPointerException.class,
                () -> servicio.transferir(ana, luis, null));

        comprobarSaldosIniciales();
    }

    private void comprobarSaldosIniciales() {
        assertEquals(new BigDecimal("100.00"), ana.getSaldo());
        assertEquals(new BigDecimal("50.00"), luis.getSaldo());
    }
}
