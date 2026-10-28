package banco.validacion;

import java.util.regex.Pattern;

// Validación de NIF de persona física: DNI (8 cifras + letra) y NIE de extranjeros
// (X, Y o Z + 7 cifras + letra). Mismo esquema que Iban: formato y luego control.
public final class Nif {
    // La letra de control es la que ocupa la posición (número % 23) en esta tabla.
    // Faltan I, Ñ, O y U a propósito: se confunden con otros caracteres.
    private static final String LETRAS = "TRWAGMYFPDXBNJZSQVHLCKE";
    // Molde: una cifra o X/Y/Z, 7 cifras más y una letra de la tabla.
    private static final Pattern FORMATO = Pattern.compile("[0-9XYZ][0-9]{7}[" + LETRAS + "]");

    private Nif() {
    }

    public static boolean valido(String nif) {
        if (nif == null) {
            return false;
        }
        // Se admite escrito con guion o espacios ("12345678-Z").
        String limpio = nif.replace(" ", "").replace("-", "").toUpperCase();
        if (!FORMATO.matcher(limpio).matches()) {
            return false;
        }
        char letraEsperada = LETRAS.charAt(numero(limpio) % 23);
        return limpio.charAt(8) == letraEsperada;
    }

    // Las 8 primeras posiciones como número. En un NIE, la letra inicial se cambia
    // por una cifra (X=0, Y=1, Z=2); el formato ya garantiza que el resto son cifras.
    private static int numero(String limpio) {
        String cifras = limpio.substring(0, 8)
                .replace('X', '0').replace('Y', '1').replace('Z', '2');
        return Integer.parseInt(cifras);
    }
}
