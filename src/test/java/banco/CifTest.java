package banco;

import banco.validacion.Cif;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CifTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "B12345674",  // sociedad limitada: control con cifra
            "A28015865",  // sociedad anónima: control con cifra
            "Q2826000H",  // organismo público: control con letra
            "B00000000",  // suma 0 -> control 0
            "C76543214",  // la C admite cifra...
            "C7654321D",  // ...o la letra equivalente (4 -> D)
            "B-12345674", // con guion
            "b12345674"   // en minúsculas
    })
    void aceptaCifValidos(String cif) {
        assertTrue(Cif.valido(cif));
    }

    // Formato correcto, pero el control no corresponde.
    @ParameterizedTest
    @ValueSource(strings = {
            "B12345675", // cifra de control equivocada
            "B21345674", // dos cifras intercambiadas
            "Q28260008", // valor correcto, pero la Q exige letra
            "A2801586E"  // valor correcto, pero la A exige cifra
    })
    void rechazaControlIncorrecto(String cif) {
        assertFalse(Cif.valido(cif));
    }

    // Fallan ya en el formato, antes de calcular el control.
    @ParameterizedTest
    @ValueSource(strings = {
            "B1234567",   // sin control
            "B123456745", // le sobra una cifra
            "I12345674",  // la I no es un tipo de entidad
            "12345678Z",  // es un DNI, no un CIF
            "B1234A674"   // letra en medio del número
    })
    void rechazaFormatoIncorrecto(String cif) {
        assertFalse(Cif.valido(cif));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaVacio(String cif) {
        assertFalse(Cif.valido(cif));
    }
}
