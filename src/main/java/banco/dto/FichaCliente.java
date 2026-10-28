package banco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// Vista 360 del cliente: sus datos, sus cuentas, sus últimos movimientos y las
// incidencias de sus transferencias, en una sola respuesta para la ficha.
public record FichaCliente(
        long id,
        String tipo,
        String nombre,
        String documento,
        String tipoDocumento,
        String estado,
        LocalDate fechaAlta,
        List<CuentaCliente> cuentas,
        List<Movimiento> movimientos,
        List<IncidenciaCliente> incidencias) {

    public record CuentaCliente(String iban, BigDecimal saldo, String moneda, String estado,
                                LocalDate fechaApertura) {
    }

    // Visto desde el cliente: SALIDA si el dinero sale de su cuenta, ENTRADA si llega.
    public record Movimiento(long transferenciaId, OffsetDateTime fecha, String ibanPropio,
                             String sentido, String ibanContraparte, String contraparte,
                             String concepto, BigDecimal importe, String moneda) {
    }

    // resueltaPor es null mientras la incidencia no esté resuelta.
    public record IncidenciaCliente(long id, String regla, BigDecimal importe, String moneda,
                                    String estado, OffsetDateTime fecha, String resueltaPor) {
    }
}
