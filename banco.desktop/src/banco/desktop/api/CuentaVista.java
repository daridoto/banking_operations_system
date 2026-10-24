package banco.desktop.api;

import java.math.BigDecimal;

public record CuentaVista(long id, String titular, BigDecimal saldo, String moneda) {
    public String etiqueta() {
        return id + " — " + titular + " (" + moneda + ")";
    }
}
