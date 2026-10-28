package banco.dto;

import banco.model.Cuenta;

import java.math.BigDecimal;

// Un record contiene datos inmutables: no exponemos la cuenta modificable.
// Hacia fuera la cuenta se identifica por IBAN; el ID interno no sale de la base.
public record CuentaRespuesta(String iban, String titular, BigDecimal saldo, String moneda, String estado) {
    public static CuentaRespuesta de(Cuenta cuenta) {
        return new CuentaRespuesta(cuenta.getIban(), cuenta.getTitular(),
                cuenta.getSaldo(), cuenta.getMoneda().getCurrencyCode(), cuenta.getEstado());
    }
}
