package banco;

import banco.validacion.Iban;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IbanTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "ES9121000418450200051332",
            "ES91 2100 0418 4502 0005 1332", // como se escribe en papel, en grupos de 4
            "es9121000418450200051332",      // en minúsculas
            "DE89370400440532013000",        // Alemania: 22 caracteres
            "GB82WEST12345698765432"         // Reino Unido: lleva letras del banco
    })
    void aceptaIbanValidos(String iban) {
        assertTrue(Iban.valido(iban));
    }

    // Formato correcto, pero el control (módulo 97) los delata.
    @ParameterizedTest
    @ValueSource(strings = {
            "ES9121000418450200051333", // una cifra cambiada al final
            "ES9121000418450200051323", // dos cifras intercambiadas
            "ES0021000418450200051332"  // dígitos de control inventados
    })
    void rechazaErroresDeTecleo(String iban) {
        assertFalse(Iban.valido(iban));
    }

    // Fallan ya en el formato, antes de llegar a la división.
    @ParameterizedTest
    @ValueSource(strings = {
            "ES912100041845020005133",   // España: le falta una cifra (23 en vez de 24)
            "ES91210004184502000513321", // España: le sobra una cifra
            "ES91A1000418450200051332",  // España: letra donde solo puede haber cifras
            "1291210004184502000513",    // el país no son letras
            "ESAB21000418450200051332",  // el control no son cifras
            "ES91-2100-0418-4502-0005-1332" // guiones: solo se admiten espacios
    })
    void rechazaFormatoIncorrecto(String iban) {
        assertFalse(Iban.valido(iban));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaVacio(String iban) {
        assertFalse(Iban.valido(iban));
    }
}
