package banco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudResolucion(
        @NotBlank @Size(max = 1000) String comentario) {
}
