package banco;

import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones a respuestas HTTP que el cliente puede entender.
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EstadoIncidenciaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> estadoIncompatible(EstadoIncidenciaException error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> operacionInvalida(IllegalArgumentException error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> recursoNoEncontrado(NoSuchElementException error) {
        return Map.of("error", error.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> datosInvalidos(MethodArgumentNotValidException error) {
        if (error.getBindingResult().getTarget() instanceof SolicitudResolucion) {
            return Map.of("error", "Indica un comentario de resolución de entre 1 y 1000 caracteres, no solo espacios.");
        }
        return Map.of("error", "Indica dos IDs positivos y un importe positivo con máximo dos decimales y 15 dígitos enteros.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> jsonInvalido() {
        return Map.of("error", "El cuerpo debe ser un JSON válido con los tipos correctos.");
    }
}
