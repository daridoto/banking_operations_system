package banco.desktop.api;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.time.OffsetDateTime;
import com.google.gson.JsonParser;

// Cliente HTTP: conoce la API, pero no los controles de la pantalla ni la base de datos.
public class BancoApi implements AutoCloseable {
    private final URI base;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public BancoApi() {
        this(URI.create("http://localhost:8080/"));
    }

    public BancoApi(URI base) {
        this.base = base;
    }

    public CompletableFuture<List<CuentaVista>> listarCuentas() {
        return enviar(solicitud("api/cuentas").GET().build());
    }

    public CompletableFuture<List<CuentaVista>> transferir(long origen, long destino, BigDecimal importe) {
        JsonObject json = new JsonObject();
        json.addProperty("cuentaOrigenId", origen);
        json.addProperty("cuentaDestinoId", destino);
        json.addProperty("importe", importe);
        return enviar(solicitud("api/transferencias")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString())).build());
    }

    public CompletableFuture<List<IncidenciaVista>> listarIncidencias() {
        return enviarJson(solicitud("api/incidencias").GET().build()).thenApply(json -> {
            List<IncidenciaVista> incidencias = new ArrayList<>();
            for (var elemento : json.getAsJsonArray()) incidencias.add(leerIncidencia(elemento));
            return List.copyOf(incidencias);
        });
    }

    public CompletableFuture<IncidenciaVista> consultarIncidencia(long id) {
        return enviarJson(solicitud("api/incidencias/" + id).GET().build())
                .thenApply(BancoApi::leerIncidencia);
    }

    public CompletableFuture<IncidenciaVista> iniciarRevision(long id) {
        return enviarJson(solicitud("api/incidencias/" + id + "/revision")
                .POST(HttpRequest.BodyPublishers.noBody()).build()).thenApply(BancoApi::leerIncidencia);
    }

    public CompletableFuture<IncidenciaVista> resolverIncidencia(long id, String comentario) {
        JsonObject json = new JsonObject();
        json.addProperty("comentario", comentario);
        return enviarJson(solicitud("api/incidencias/" + id + "/resolucion")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString())).build()).thenApply(BancoApi::leerIncidencia);
    }

    public CompletableFuture<List<CambioIncidenciaVista>> consultarHistorialIncidencia(long id) {
        return enviarJson(solicitud("api/incidencias/" + id + "/historial").GET().build()).thenApply(json -> {
            List<CambioIncidenciaVista> cambios = new ArrayList<>();
            for (var elemento : json.getAsJsonArray()) {
                var c = elemento.getAsJsonObject();
                var comentario = c.get("comentario");
                cambios.add(new CambioIncidenciaVista(c.get("id").getAsLong(),
                        c.get("incidenciaId").getAsLong(), c.get("estadoAnterior").getAsString(),
                        c.get("estadoNuevo").getAsString(), OffsetDateTime.parse(c.get("fecha").getAsString()),
                        comentario == null || comentario.isJsonNull() ? null : comentario.getAsString()));
            }
            return List.copyOf(cambios);
        });
    }

    private static IncidenciaVista leerIncidencia(JsonElement elemento) {
        var i = elemento.getAsJsonObject();
        return new IncidenciaVista(i.get("id").getAsLong(), i.get("transferenciaId").getAsLong(),
                i.get("regla").getAsString(), i.get("motivo").getAsString(),
                i.get("prioridad").getAsString(), i.get("estado").getAsString(),
                OffsetDateTime.parse(i.get("fecha").getAsString()));
    }

    private HttpRequest.Builder solicitud(String ruta) {
        return HttpRequest.newBuilder(base.resolve(ruta)).timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
    }

    private CompletableFuture<JsonElement> enviarJson(HttpRequest solicitud) {
        return cliente.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString()).thenApply(respuesta -> {
            if (respuesta.statusCode() != 200) {
                String mensaje = "El servidor devolvió HTTP " + respuesta.statusCode() + ".";
                try {
                    var error = JsonParser.parseString(respuesta.body()).getAsJsonObject().get("error");
                    if (error != null && !error.isJsonNull()) mensaje = error.getAsString();
                } catch (RuntimeException ignorado) {
                    // Si no es JSON, mostramos el código HTTP.
                }
                throw new ErrorApi(respuesta.statusCode(), mensaje);
            }
            return JsonParser.parseString(respuesta.body());
        });
    }

    private CompletableFuture<List<CuentaVista>> enviar(HttpRequest solicitud) {
        return enviarJson(solicitud).thenApply(json -> {
            List<CuentaVista> cuentas = new ArrayList<>();
            for (var elemento : json.getAsJsonArray()) {
                var c = elemento.getAsJsonObject();
                cuentas.add(new CuentaVista(c.get("id").getAsLong(), c.get("titular").getAsString(),
                        c.get("saldo").getAsBigDecimal(), c.get("moneda").getAsString()));
            }
            return List.copyOf(cuentas);
        });
    }

    public static class ErrorApi extends RuntimeException {
        private final int codigo;
        public ErrorApi(int codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }
        public int codigo() { return codigo; }
    }

    @Override
    public void close() {
        cliente.shutdownNow();
    }
}
