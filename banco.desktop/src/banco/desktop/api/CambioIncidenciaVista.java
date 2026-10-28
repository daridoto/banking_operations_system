package banco.desktop.api;

import java.time.OffsetDateTime;

public record CambioIncidenciaVista(long id, long incidenciaId, String estadoAnterior,
                                   String estadoNuevo, OffsetDateTime fecha, String comentario,
                                   String usuario) {
}
