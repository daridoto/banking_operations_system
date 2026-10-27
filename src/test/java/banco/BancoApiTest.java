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
@WithMockUser
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
                        {"cuentaOrigenId":1,"cuentaDestinoId":2,"importe":20.00}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saldo").value(80))
                .andExpect(jsonPath("$[1].saldo").value(70));

        api.perform(get("/api/transferencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cuentaOrigenId").value(1))
                .andExpect(jsonPath("$[0].cuentaDestinoId").value(2))
                .andExpect(jsonPath("$[0].importe").value(20))
                .andExpect(jsonPath("$[0].fecha").isNotEmpty());

        api.perform(get("/api/cuentas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saldo").value(80))
                .andExpect(jsonPath("$[1].saldo").value(70));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"cuentaOrigenId\":1,\"cuentaDestinoId\":2,\"importe\":100.01}",
            "{\"cuentaOrigenId\":1,\"cuentaDestinoId\":1,\"importe\":20}",
            "{\"cuentaOrigenId\":1,\"cuentaDestinoId\":2,\"importe\":-20}",
            "{\"cuentaOrigenId\":1,\"cuentaDestinoId\":2,\"importe\":0}",
            "{\"cuentaOrigenId\":1,\"cuentaDestinoId\":2,\"importe\":0.001}",
            "{\"cuentaOrigenId\":1,\"cuentaDestinoId\":2}",
            "{\"cuentaOrigenId\":1.5,\"cuentaDestinoId\":2,\"importe\":20}",
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
                        {"cuentaOrigenId":1,"cuentaDestinoId":999,"importe":20}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe la cuenta 999."));
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
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].saldo").value(100))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].saldo").value(50));
    }
}
