package banco.desktop.api;

import java.math.BigDecimal;

// La cuenta se identifica por IBAN, como la devuelve la API (sin espacios).
public record CuentaVista(String iban, String titular, BigDecimal saldo, String moneda) {

    // En pantalla, el IBAN se muestra en grupos de 4 para leerlo sin errores.
    public String ibanFormateado() {
        return formatearIban(iban);
    }

    public String etiqueta() {
        return titular + " — " + ibanFormateado() + " (" + moneda + ")";
    }

    public static String formatearIban(String iban) {
        return iban == null ? "" : iban.replaceAll("(.{4})(?!$)", "$1 ");
    }
}
