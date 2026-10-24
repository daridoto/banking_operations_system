package banco.desktop.api;

import java.time.OffsetDateTime;

public record IncidenciaVista(long id, long transferenciaId, String regla, String motivo,
                              String prioridad, String estado, OffsetDateTime fecha) {
}
