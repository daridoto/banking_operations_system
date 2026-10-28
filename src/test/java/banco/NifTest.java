package banco;

import banco.validacion.Nif;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NifTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678Z",
            "87654321X",
            "00000000T",  // resto 0 -> T
            "12345678-Z", // con guion, como suele escribirse
            "12345678z",  // en minúsculas
            "X1234567L",  // NIE: la X cuenta como 0
            "Y1234567X",  // NIE: la Y cuenta como 1
            "Z1234567R"   // NIE: la Z cuenta como 2
    })
    void aceptaNifYNieValidos(String nif) {
        assertTrue(Nif.valido(nif));
    }

    // Formato correcto, pero la letra no corresponde al número.
    @ParameterizedTest
    @ValueSource(strings = {
            "12345678A", // letra equivocada
            "12345687Z", // dos cifras intercambiadas
            "X1234567R"  // NIE con la letra de otro prefijo (la de Z1234567)
    })
    void rechazaLetraIncorrecta(String nif) {
        assertFalse(Nif.valido(nif));
    }

    // Fallan ya en el formato, antes de calcular la letra.
    @ParameterizedTest
    @ValueSource(strings = {
            "1234567Z",   // le falta una cifra
            "123456789Z", // le sobra una cifra
            "12345678",   // sin letra
            "12345678U",  // la U no se usa nunca como letra de control
            "A1234567L",  // prefijo de NIE que no existe
            "12A45678Z"   // letra en medio del número
    })
    void rechazaFormatoIncorrecto(String nif) {
        assertFalse(Nif.valido(nif));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaVacio(String nif) {
        assertFalse(Nif.valido(nif));
    }
}
