package banco.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// Incidencia con los datos de su transferencia (importe, quién envía, quién recibe y
// concepto), para que el revisor tenga el contexto sin buscarlo en otra pantalla.
public record Incidencia(long id, long transferenciaId, String regla, String motivo,
                         String prioridad, String estado, OffsetDateTime fecha,
                         BigDecimal importe, String moneda, String ordenante,
                         String beneficiario, String concepto) {
}
