package banco;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Login con JWT: credenciales correctas devuelven un token; sin token no se entra.
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthTest {
    @Autowired
    private MockMvc api;

    @Test
    void loginCorrectoDevuelveToken() throws Exception {
        api.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"empleado\",\"password\":\"empleado123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginIncorrectoDevuelve401() throws Exception {
        api.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"empleado\",\"password\":\"malamala\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenNoSePuedeConsultar() throws Exception {
        api.perform(get("/api/cuentas")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenSePuedeConsultar() throws Exception {
        String token = login("empleado", "empleado123");
        api.perform(get("/api/cuentas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String login(String usuario, String password) throws Exception {
        String respuesta = api.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"" + usuario + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(respuesta, "$.token");
    }
}
