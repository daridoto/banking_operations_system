package banco.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Transferencia(long id, long cuentaOrigenId, long cuentaDestinoId,
                            BigDecimal importe, String moneda, OffsetDateTime fecha) {
}
