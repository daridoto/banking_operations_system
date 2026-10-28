package banco.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Transferencia(long id, String ibanOrigen, String ibanDestino,
                            BigDecimal importe, String moneda, String concepto,
                            OffsetDateTime fecha, String creadaPor) {
}
