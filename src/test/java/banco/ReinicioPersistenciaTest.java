package banco;

import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReinicioPersistenciaTest {
    @TempDir Path directorio;

    @Test
    void conservaDatosAlCerrarYReabrirLaAplicacion() {
        String url = "jdbc:h2:file:" + directorio.resolve("banco").toAbsolutePath();
        try (var primera = arrancar(url)) {
            primera.getBean(BancoService.class).transferir(
                    new SolicitudTransferencia(1L, 2L, new BigDecimal("60.00")));
            BancoService banco = primera.getBean(BancoService.class);
            banco.iniciarRevision(banco.listarIncidencias().get(0).id());
            // Simula el esquema anterior a la incorporación del comentario.
            primera.getBean(JdbcTemplate.class).execute("ALTER TABLE cambios_incidencia DROP COLUMN comentario");
        }
        // Abre de nuevo el archivo y vuelve a ejecutar schema.sql y data.sql.
        try (var segunda = arrancar(url)) {
            BancoService banco = segunda.getBean(BancoService.class);
            var cuentas = banco.listarCuentas();
            assertEquals(2, cuentas.size());
            assertEquals(new BigDecimal("40.00"), cuentas.get(0).saldo());
            assertEquals(new BigDecimal("110.00"), cuentas.get(1).saldo());
            assertEquals(1, banco.listarTransferencias().size());
            assertEquals(1, banco.listarIncidencias().size());
            assertEquals(banco.listarTransferencias().get(0).id(),
                    banco.listarIncidencias().get(0).transferenciaId());
            assertEquals("EN_REVISION", banco.listarIncidencias().get(0).estado());
            banco.resolverIncidencia(banco.listarIncidencias().get(0).id(), "Operación justificada.");
        }
        try (var tercera = arrancar(url)) {
            assertEquals("RESUELTA", tercera.getBean(BancoService.class).listarIncidencias().get(0).estado());
            var cambios = tercera.getBean(JdbcTemplate.class)
                    .queryForList("SELECT * FROM cambios_incidencia ORDER BY id");
            assertEquals(2, cambios.size());
            assertEquals("Operación justificada.", cambios.get(1).get("COMENTARIO"));
            org.junit.jupiter.api.Assertions.assertNotNull(cambios.get(1).get("FECHA"));
        }
    }

    private ConfigurableApplicationContext arrancar(String url) {
        return new SpringApplicationBuilder(Main.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + url, "--banco.deteccion.umbral-eur=50.00");
    }
}
