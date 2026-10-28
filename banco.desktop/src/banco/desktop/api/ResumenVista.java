package banco.desktop.api;

import java.math.BigDecimal;

// Indicadores de GET /api/resumen.
public record ResumenVista(long transferenciasHoy, BigDecimal importeHoyEur,
                           long incidenciasPendientes, long cuentasBloqueadas) {
}
