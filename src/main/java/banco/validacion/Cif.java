package banco.validacion;

import java.util.regex.Pattern;

// Validación de CIF (NIF de persona jurídica): letra de tipo de entidad + 7 cifras
// + control, que según el tipo es una cifra o una letra. Mismo esquema que Nif.
public final class Cif {
    // Molde: tipo de entidad (A sociedad anónima, B limitada, Q organismo público...),
    // 7 cifras y un control que puede ser cifra o letra de la tabla de abajo.
    private static final Pattern FORMATO = Pattern.compile("[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]");
    // Control en forma de letra: 0 -> J, 1 -> A, 2 -> B ... 9 -> I.
    private static final String LETRAS_CONTROL = "JABCDEFGHI";
    // Estos tipos llevan siempre cifra; estos otros, siempre letra; el resto, cualquiera.
    private static final String SOLO_CIFRA = "ABEH";
    private static final String SOLO_LETRA = "NPQRSW";

    private Cif() {
    }

    public static boolean valido(String cif) {
        if (cif == null) {
            return false;
        }
        String limpio = cif.replace(" ", "").replace("-", "").toUpperCase();
        if (!FORMATO.matcher(limpio).matches()) {
            return false;
        }
        char tipo = limpio.charAt(0);
        char control = limpio.charAt(8);
        int esperado = digitoControl(limpio.substring(1, 8));

        boolean esCifra = Character.isDigit(control);
        if (esCifra && SOLO_LETRA.indexOf(tipo) >= 0) {
            return false;
        }
        if (!esCifra && SOLO_CIFRA.indexOf(tipo) >= 0) {
            return false;
        }
        return esCifra
                ? control - '0' == esperado
                : control == LETRAS_CONTROL.charAt(esperado);
    }

    // Pares (2.ª, 4.ª, 6.ª) se suman tal cual; impares (1.ª, 3.ª, 5.ª, 7.ª) se doblan
    // y se suman sus cifras (7 x 2 = 14 -> 1 + 4 = 5). El control es lo que le falta
    // a la última cifra del total para llegar a 10 (y 0 si ya acaba en 0).
    private static int digitoControl(String cifras) {
        int suma = 0;
        for (int i = 0; i < cifras.length(); i++) {
            int cifra = cifras.charAt(i) - '0';
            boolean posicionImpar = i % 2 == 0; // i empieza en 0: i=0 es la 1.ª posición
            if (posicionImpar) {
                int doble = cifra * 2;
                suma += doble / 10 + doble % 10;
            } else {
                suma += cifra;
            }
        }
        return (10 - suma % 10) % 10;
    }
}
