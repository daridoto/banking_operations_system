package banco.validacion;

import java.math.BigInteger;
import java.util.regex.Pattern;

// Validación de IBAN en dos niveles: formato (el "molde") y control (módulo 97).
// Java puro: lo usan la API, la importación de remesas y los datos de ejemplo.
public final class Iban {
    // Molde general: 2 letras de país, 2 cifras de control y de 11 a 30 letras o cifras.
    private static final Pattern FORMATO = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}");
    // Molde español: ES + 2 de control + 20 cifras (banco, oficina, control y cuenta).
    private static final Pattern FORMATO_ES = Pattern.compile("ES[0-9]{22}");
    private static final BigInteger NOVENTA_Y_SIETE = BigInteger.valueOf(97);

    private Iban() {
    }

    // En papel se escribe en grupos de 4 ("ES91 2100 ..."); en el sistema se guarda
    // sin espacios y en mayúsculas, que es el formato electrónico.
    public static String normalizar(String iban) {
        return iban == null ? null : iban.replace(" ", "").toUpperCase();
    }

    public static boolean valido(String iban) {
        if (iban == null) {
            return false;
        }
        String limpio = normalizar(iban);
        if (!FORMATO.matcher(limpio).matches()) {
            return false;
        }
        if (limpio.startsWith("ES") && !FORMATO_ES.matcher(limpio).matches()) {
            return false;
        }
        return resto97(limpio) == 1;
    }

    // Los 4 primeros caracteres van al final, cada letra pasa a número (A=10 ... Z=35)
    // y se calcula el resto de dividir entre 97. El número resultante tiene unas 30
    // cifras y no cabe en un long (máx. 19), por eso se usa BigInteger.
    private static int resto97(String iban) {
        String reordenado = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numero = new StringBuilder();
        for (char c : reordenado.toCharArray()) {
            numero.append(Character.getNumericValue(c)); // '7' -> "7", 'E' -> "14", 'S' -> "28"
        }
        return new BigInteger(numero.toString()).mod(NOVENTA_Y_SIETE).intValue();
    }
}
