package banco;

import banco.service.ReglaImporteElevado;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReglaImporteElevadoTest {
    @Test
    void soloDetectaImportesEurEstrictamenteSuperioresAlUmbral() {
        var regla = new ReglaImporteElevado(new BigDecimal("50.00"));
        assertFalse(regla.seCumple(new BigDecimal("49.99"), "EUR"));
        assertFalse(regla.seCumple(new BigDecimal("50.00"), "EUR"));
        assertTrue(regla.seCumple(new BigDecimal("50.01"), "EUR"));
        assertFalse(regla.seCumple(new BigDecimal("60.00"), "USD"));
    }

    @Test
    void utilizaElUmbralConfiguradoYLoExplicaEnElMotivo() {
        var regla = new ReglaImporteElevado(new BigDecimal("75.00"));
        assertFalse(regla.seCumple(new BigDecimal("60.00"), "EUR"));
        assertTrue(regla.seCumple(new BigDecimal("80.00"), "EUR"));
        assertTrue(regla.motivo(new BigDecimal("80.00")).contains("75.00 EUR"));
        assertThrows(IllegalArgumentException.class,
                () -> new ReglaImporteElevado(BigDecimal.ZERO));
    }
}
