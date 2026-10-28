package banco.desktop.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Incidencia con el contexto de su transferencia (importe, ordenante, beneficiario, concepto).
public record IncidenciaVista(long id, long transferenciaId, String regla, String motivo,
                              String prioridad, String estado, OffsetDateTime fecha,
                              BigDecimal importe, String moneda, String ordenante,
                              String beneficiario, String concepto) {
}
