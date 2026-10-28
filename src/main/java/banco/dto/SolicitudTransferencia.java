package banco.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Las cuentas se indican por IBAN, como en una transferencia real. El dígito de
// control lo comprueba el servicio con Iban.valido. El concepto es opcional (SEPA).
public record SolicitudTransferencia(
        @NotBlank String ibanOrigen,
        @NotBlank String ibanDestino,
        @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal importe,
        @Size(max = 140) String concepto) {
}
