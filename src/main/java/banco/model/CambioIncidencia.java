package banco.model;

import java.time.OffsetDateTime;

public record CambioIncidencia(long id, long incidenciaId, String estadoAnterior,
                              String estadoNuevo, OffsetDateTime fecha, String comentario,
                              String usuario, String nombreUsuario) {
}
