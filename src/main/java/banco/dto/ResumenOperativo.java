package banco.dto;

import java.math.BigDecimal;

// Indicadores de la pestaña Cuentas: el volumen de trabajo del día y lo que necesita
// atención. El importe se suma solo en euros (no se mezclan monedas).
public record ResumenOperativo(long transferenciasHoy, BigDecimal importeHoyEur,
                               long incidenciasPendientes, long cuentasBloqueadas) {
}
