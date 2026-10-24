package banco;

import java.math.BigDecimal;
import java.util.Objects;

public class ServicioTransferencias {
    public void transferir(Cuenta origen, Cuenta destino, BigDecimal importe) {
        Objects.requireNonNull(origen, "La cuenta de origen es obligatoria.");
        Objects.requireNonNull(destino, "La cuenta de destino es obligatoria.");
        if (origen.getId() == destino.getId()) {
            throw new IllegalArgumentException("Las cuentas deben ser distintas.");
        }
        if (!origen.getMoneda().equals(destino.getMoneda())) {
            throw new IllegalArgumentException("Las cuentas deben tener la misma moneda.");
        }

        // retirar valida el importe y el saldo antes de modificar la cuenta.
        // BancoService envuelve esta lógica y las escrituras SQL en una transacción.
        origen.retirar(importe);
        destino.ingresar(importe);
    }
}
