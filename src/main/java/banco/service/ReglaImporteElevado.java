package banco.service;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReglaImporteElevado {
    public static final String CODIGO = "IMPORTE_ELEVADO";
    private final BigDecimal umbral;

    public ReglaImporteElevado(@Value("${banco.deteccion.umbral-eur}") BigDecimal umbral) {
        if (umbral == null || umbral.signum() <= 0) {
            throw new IllegalArgumentException("El umbral de detección debe ser positivo.");
        }
        this.umbral = umbral;
    }

    // Regla didáctica solo para EUR. Detectar no significa rechazar ni probar fraude.
    public boolean seCumple(BigDecimal importe, String moneda) {
        return "EUR".equals(moneda) && importe.compareTo(umbral) > 0;
    }

    public String motivo(BigDecimal importe) {
        return "Importe " + importe.toPlainString() + " EUR superior al umbral de "
                + umbral.toPlainString() + " EUR. Requiere revisión.";
    }
}
