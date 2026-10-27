package banco.dto;

import banco.model.Cuenta;

import java.math.BigDecimal;

// Un record contiene datos inmutables: no exponemos la cuenta modificable.
public record CuentaRespuesta(long id, String titular, BigDecimal saldo, String moneda) {
    public static CuentaRespuesta de(Cuenta cuenta) {
        return new CuentaRespuesta(cuenta.getId(), cuenta.getTitular(),
                cuenta.getSaldo(), cuenta.getMoneda().getCurrencyCode());
    }
}
