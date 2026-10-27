package banco.model;

import java.time.OffsetDateTime;

public record Incidencia(long id, long transferenciaId, String regla, String motivo,
                         String prioridad, String estado, OffsetDateTime fecha) {
}
