package banco.desktop.api;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

    // Indicadores de la pestaña Cuentas: actividad de hoy y lo que requiere atención.
    public CompletableFuture<ResumenVista> consultarResumen() {
        return enviarJson(solicitud("api/resumen").GET().build()).thenApply(json -> {
            var r = json.getAsJsonObject();
            return new ResumenVista(r.get("transferenciasHoy").getAsLong(), r.get("importeHoyEur").getAsBigDecimal(),
                    r.get("incidenciasPendientes").getAsLong(), r.get("cuentasBloqueadas").getAsLong());
        });
    }

    public CompletableFuture<List<CuentaVista>> listarCuentas() {
        return enviar(solicitud("api/cuentas").GET().build());
    }

    // Envía usuario y contraseña una sola vez y devuelve el token JWT.
    // Contraseña incorrecta: el backend responde 401 y llega como ErrorApi.
    public CompletableFuture<String> login(String usuario, String password) {
        JsonObject json = new JsonObject();
        json.addProperty("usuario", usuario);
        json.addProperty("password", password);
        return enviarJson(solicitud("api/login")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString())).build())
                .thenApply(respuesta -> respuesta.getAsJsonObject().get("token").getAsString());
    }

    // "Quién soy": nombre completo y rol del usuario al que pertenece el token.
    public CompletableFuture<JsonObject> consultarYo() {
        return enviarJson(solicitud("api/me").GET().build()).thenApply(JsonElement::getAsJsonObject);
    }

    // El backend valida los IBAN (formato y dígito de control) y responde 400 si fallan.
    public CompletableFuture<List<CuentaVista>> transferir(String ibanOrigen, String ibanDestino,
                                                          BigDecimal importe, String concepto) {
        JsonObject json = new JsonObject();
        json.addProperty("ibanOrigen", ibanOrigen);
        json.addProperty("ibanDestino", ibanDestino);
        json.addProperty("importe", importe);
        json.addProperty("concepto", concepto);
        return enviar(solicitud("api/transferencias")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString())).build());
    }

    // El texto va codificado en la URL: "García López" viaja como "Garc%C3%ADa+L%C3%B3pez".
    public CompletableFuture<List<ClienteVista>> buscarClientes(String texto) {
        String q = URLEncoder.encode(texto, StandardCharsets.UTF_8);
        return enviarJson(solicitud("api/clientes?q=" + q).GET().build()).thenApply(json -> {
            List<ClienteVista> clientes = new ArrayList<>();
            for (var elemento : json.getAsJsonArray()) {
                var c = elemento.getAsJsonObject();
                clientes.add(new ClienteVista(c.get("id").getAsLong(), c.get("tipo").getAsString(),
                        c.get("nombre").getAsString(), c.get("documento").getAsString(),
                        c.get("estado").getAsString()));
            }
            return List.copyOf(clientes);
        });
    }

    public CompletableFuture<FichaClienteVista> consultarCliente(long id) {
        return enviarJson(solicitud("api/clientes/" + id).GET().build()).thenApply(json -> {
            var f = json.getAsJsonObject();
            List<FichaClienteVista.Cuenta> cuentas = new ArrayList<>();
            for (var e : f.getAsJsonArray("cuentas")) {
                var c = e.getAsJsonObject();
                cuentas.add(new FichaClienteVista.Cuenta(c.get("iban").getAsString(),
                        c.get("saldo").getAsBigDecimal(), c.get("moneda").getAsString(),
                        c.get("estado").getAsString(), LocalDate.parse(c.get("fechaApertura").getAsString())));
            }
            List<FichaClienteVista.Movimiento> movimientos = new ArrayList<>();
            for (var e : f.getAsJsonArray("movimientos")) {
                var m = e.getAsJsonObject();
                movimientos.add(new FichaClienteVista.Movimiento(
                        OffsetDateTime.parse(m.get("fecha").getAsString()), m.get("ibanPropio").getAsString(),
                        m.get("sentido").getAsString(), m.get("contraparte").getAsString(),
                        textoOpcional(m, "concepto"), m.get("importe").getAsBigDecimal(),
                        m.get("moneda").getAsString()));
            }
            List<FichaClienteVista.Incidencia> incidencias = new ArrayList<>();
            for (var e : f.getAsJsonArray("incidencias")) {
                var i = e.getAsJsonObject();
                incidencias.add(new FichaClienteVista.Incidencia(i.get("id").getAsLong(),
                        i.get("regla").getAsString(), i.get("importe").getAsBigDecimal(),
                        i.get("moneda").getAsString(), i.get("estado").getAsString(),
                        textoOpcional(i, "resueltaPor")));
            }
            return new FichaClienteVista(f.get("id").getAsLong(), f.get("tipo").getAsString(),
                    f.get("nombre").getAsString(), f.get("documento").getAsString(),
                    f.get("tipoDocumento").getAsString(), f.get("estado").getAsString(),
                    LocalDate.parse(f.get("fechaAlta").getAsString()),
                    List.copyOf(cuentas), List.copyOf(movimientos), List.copyOf(incidencias));
        });
    }

    // Campos que pueden venir como null en el JSON (concepto, resueltaPor...).
    private static String textoOpcional(JsonObject objeto, String campo) {
        var valor = objeto.get(campo);
        return valor == null || valor.isJsonNull() ? null : valor.getAsString();
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
                var usuario = c.get("usuario");
                cambios.add(new CambioIncidenciaVista(c.get("id").getAsLong(),
                        c.get("incidenciaId").getAsLong(), c.get("estadoAnterior").getAsString(),
                        c.get("estadoNuevo").getAsString(), OffsetDateTime.parse(c.get("fecha").getAsString()),
                        comentario == null || comentario.isJsonNull() ? null : comentario.getAsString(),
                        usuario == null || usuario.isJsonNull() ? null : usuario.getAsString(),
                        textoOpcional(c, "nombreUsuario")));
            }
            return List.copyOf(cambios);
        });
    }

    private static IncidenciaVista leerIncidencia(JsonElement elemento) {
        var i = elemento.getAsJsonObject();
        return new IncidenciaVista(i.get("id").getAsLong(), i.get("transferenciaId").getAsLong(),
                i.get("regla").getAsString(), i.get("motivo").getAsString(),
                i.get("prioridad").getAsString(), i.get("estado").getAsString(),
                OffsetDateTime.parse(i.get("fecha").getAsString()),
                i.get("importe").getAsBigDecimal(), i.get("moneda").getAsString(),
                i.get("ordenante").getAsString(), i.get("beneficiario").getAsString(),
                textoOpcional(i, "concepto"));
    }

    private HttpRequest.Builder solicitud(String ruta) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve(ruta))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
        // Todas las peticiones pasan por aquí: añadimos el token en un solo sitio.
        // Antes del login no hay token, así que /api/login sale sin cabecera.
        if (Sesion.hayToken()) {
            builder.header("Authorization", Sesion.cabeceraBearer());
        }
        return builder;
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
                cuentas.add(new CuentaVista(c.get("iban").getAsString(), c.get("titular").getAsString(),
                        c.get("saldo").getAsBigDecimal(), c.get("moneda").getAsString(),
                        c.get("estado").getAsString()));
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
