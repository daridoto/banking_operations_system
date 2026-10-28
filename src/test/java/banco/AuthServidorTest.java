package banco;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Contra un servidor real (Tomcat), no MockMvc: MockMvc no reenvía los errores a
// /error, que es donde un 403 se convertía en 401. Con token real, no @WithMockUser.
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class AuthServidorTest {
    @LocalServerPort
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    @Test
    void empleadoConTokenNoPuedeResolver() throws Exception {
        String token = login("empleado", "empleado123");
        var respuesta = enviar(HttpRequest.newBuilder(url("/api/incidencias/1/resolucion"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"comentario\":\"x\"}")).build());
        assertEquals(403, respuesta.statusCode());
    }

    @Test
    void sinTokenSigueSiendo401() throws Exception {
        var respuesta = enviar(HttpRequest.newBuilder(url("/api/cuentas")).GET().build());
        assertEquals(401, respuesta.statusCode());
    }

    private String login(String usuario, String password) throws Exception {
        var respuesta = enviar(HttpRequest.newBuilder(url("/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"usuario\":\"" + usuario + "\",\"password\":\"" + password + "\"}")).build());
        assertEquals(200, respuesta.statusCode());
        return com.jayway.jsonpath.JsonPath.read(respuesta.body(), "$.token");
    }

    private HttpResponse<String> enviar(HttpRequest peticion) throws Exception {
        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
