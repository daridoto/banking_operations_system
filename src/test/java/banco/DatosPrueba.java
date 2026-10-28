package banco;

// IBAN de las dos cuentas de db/prueba/V1_1__datos_prueba.sql (100 y 50 EUR).
final class DatosPrueba {
    static final String IBAN_ANA = "ES6099990001410000123456";
    static final String IBAN_LUIS = "ES2599990001450000234567";
    // Válido, pero de otro banco: no existe en Countercheck.
    static final String IBAN_DESCONOCIDO = "ES9121000418450200051332";

    private DatosPrueba() {
    }
}
