package banco.desktop.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// Ficha completa de un cliente (GET /api/clientes/{id}).
public record FichaClienteVista(long id, String tipo, String nombre, String documento,
                                String tipoDocumento, String estado, LocalDate fechaAlta,
                                List<Cuenta> cuentas, List<Movimiento> movimientos,
                                List<Incidencia> incidencias) {

    public boolean esEmpresa() {
        return "JURIDICA".equals(tipo);
    }

    public record Cuenta(String iban, BigDecimal saldo, String moneda, String estado,
                         LocalDate fechaApertura) {
    }

    // sentido: SALIDA si el dinero sale de la cuenta del cliente, ENTRADA si llega.
    public record Movimiento(OffsetDateTime fecha, String ibanPropio, String sentido,
                             String contraparte, String concepto, BigDecimal importe, String moneda) {
        public boolean esSalida() {
            return "SALIDA".equals(sentido);
        }
    }

    public record Incidencia(long id, String regla, BigDecimal importe, String moneda,
                             String estado, String resueltaPor) {
    }
}
