package banco.desktop.parts;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import org.eclipse.swt.graphics.Color;

// Colores y formatos comunes a todas las pestañas, para que se vean coherentes.
final class Estilo {
    static final Locale ESPANA = Locale.forLanguageTag("es-ES");
    static final Color VERDE = new Color(46, 125, 50);
    static final Color ROJO = new Color(183, 28, 28);
    static final Color NARANJA = new Color(230, 126, 34);
    static final Color AZUL = new Color(25, 118, 210);
    static final Color GRIS = new Color(117, 117, 117);

    private Estilo() {
    }

    // Formato español: 2.600,00 €
    static String dinero(BigDecimal importe, String moneda) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(ESPANA);
        formato.setCurrency(Currency.getInstance(moneda));
        return formato.format(importe);
    }

    // Color del estado de una cuenta: verde si opera con normalidad, rojo si no.
    static Color colorEstadoCuenta(String estado) {
        return "ACTIVA".equals(estado) ? VERDE : ROJO;
    }
}
