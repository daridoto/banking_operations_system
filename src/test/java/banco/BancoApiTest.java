package banco;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integra Spring, JSON, validación, controlador y servicios, sin abrir un puerto.
// @WithMockUser ejecuta cada petición como un usuario autenticado de prueba.
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "empleado")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BancoApiTest {
    @Autowired
    private MockMvc api;

    @Test
    void consultaLasCuentasIniciales() throws Exception {
        comprobarSaldosIniciales();
        comprobarHistorialVacio();
    }

    @Test
    void transfiereYConservaLosSaldosEntrePeticiones() throws Exception {
        api.perform(post("/api/transferencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ibanOrigen":"ES6099990001410000123456","ibanDestino":"ES2599990001450000234567","importe":20.00,"concepto":"Alquiler octubre"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saldo").value(80))
                .andExpect(jsonPath("$[1].saldo").value(70));

        api.perform(get("/api/transferencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ibanOrigen").value(DatosPrueba.IBAN_ANA))
                .andExpect(jsonPath("$[0].ibanDestino").value(DatosPrueba.IBAN_LUIS))
                .andExpect(jsonPath("$[0].concepto").value("Alquiler octubre"))
                .andExpect(jsonPath("$[0].importe").value(20))
                .andExpect(jsonPath("$[0].fecha").isNotEmpty())
                // Auditoría: queda registrado el usuario autenticado que la hizo.
                .andExpect(jsonPath("$[0].creadaPor").value("empleado"));

        api.perform(get("/api/cuentas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saldo").value(80))
                .andExpect(jsonPath("$[1].saldo").value(70));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"ibanOrigen\":\"ES6099990001410000123456\",\"ibanDestino\":\"ES2599990001450000234567\",\"importe\":100.01}",
            "{\"ibanOrigen\":\"ES6099990001410000123456\",\"ibanDestino\":\"ES6099990001410000123456\",\"importe\":20}",
            "{\"ibanOrigen\":\"ES6099990001410000123456\",\"ibanDestino\":\"ES2599990001450000234567\",\"importe\":-20}",
            "{\"ibanOrigen\":\"ES6099990001410000123456\",\"ibanDestino\":\"ES2599990001450000234567\",\"importe\":0}",
            "{\"ibanOrigen\":\"ES6099990001410000123456\",\"ibanDestino\":\"ES2599990001450000234567\",\"importe\":0.001}",
            "{\"ibanOrigen\":\"ES6099990001410000123456\",\"ibanDestino\":\"ES2599990001450000234567\"}",
            // IBAN con una cifra cambiada: falla el dígito de control.
            "{\"ibanOrigen\":\"ES6099990001410000123457\",\"ibanDestino\":\"ES2599990001450000234567\",\"importe\":20}",
            // El mismo IBAN escrito con espacios cuenta como la misma cuenta.
            "{\"ibanOrigen\":\"ES6099990001410000123456\",\"ibanDestino\":\"ES60 9999 0001 4100 0012 3456\",\"importe\":20}",
            "{\"ibanOrigen\":\"ES6099990001410000123456\",\"ibanDestino\":\"ES2599990001450000234567\",\"importe\":20,\"concepto\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
            "{}", "null", "no es json"
    })
    void rechazaPeticionesInvalidasSinCambiarSaldos(String json) throws Exception {
        api.perform(post("/api/transferencias")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
        comprobarSaldosIniciales();
        comprobarHistorialVacio();
    }

    @Test
    void devuelve404SiLaCuentaNoExiste() throws Exception {
        api.perform(post("/api/transferencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ibanOrigen":"ES6099990001410000123456","ibanDestino":"ES9121000418450200051332","importe":20}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe ninguna cuenta con IBAN ES9121000418450200051332."));
        comprobarSaldosIniciales();
        comprobarHistorialVacio();
    }

    private void comprobarHistorialVacio() throws Exception {
        api.perform(get("/api/transferencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void comprobarSaldosIniciales() throws Exception {
        api.perform(get("/api/cuentas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].iban").value(DatosPrueba.IBAN_ANA))
                .andExpect(jsonPath("$[0].titular").value("Ana García López"))
                .andExpect(jsonPath("$[0].saldo").value(100))
                .andExpect(jsonPath("$[1].iban").value(DatosPrueba.IBAN_LUIS))
                .andExpect(jsonPath("$[1].saldo").value(50));
    }
}
