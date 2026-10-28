package banco.model;

import banco.validacion.Iban;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public class Cuenta {
    private final long id;
    private final String iban;
    private final String titular;
    private BigDecimal saldo;
    private final Currency moneda;

    public Cuenta(long id, String iban, String titular, BigDecimal saldo, String moneda) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser positivo.");
        }
        if (!Iban.valido(iban)) {
            throw new IllegalArgumentException("El IBAN no es válido.");
        }
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("El titular es obligatorio.");
        }
        Objects.requireNonNull(saldo, "El saldo es obligatorio.");
        if (saldo.signum() < 0) {
            throw new IllegalArgumentException("El saldo no puede ser negativo.");
        }
        this.id = id;
        this.iban = Iban.normalizar(iban);
        this.titular = titular;
        this.saldo = saldo;
        this.moneda = Currency.getInstance(moneda);
    }

    public long getId() {
        return id;
    }

    public String getIban() {
        return iban;
    }

    public String getTitular() {
        return titular;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public Currency getMoneda() {
        return moneda;
    }

    public void ingresar(BigDecimal importe) {
        validarImporte(importe);
        saldo = saldo.add(importe);
    }

    public void retirar(BigDecimal importe) {
        validarImporte(importe);
        if (saldo.compareTo(importe) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente.");
        }
        saldo = saldo.subtract(importe);
    }

    private void validarImporte(BigDecimal importe) {
        Objects.requireNonNull(importe, "El importe es obligatorio.");
        if (importe.signum() <= 0) {
            throw new IllegalArgumentException("El importe debe ser positivo.");
        }
    }
}
