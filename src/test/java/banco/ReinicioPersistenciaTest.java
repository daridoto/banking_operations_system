package banco;

import static banco.DatosPrueba.IBAN_ANA;
import static banco.DatosPrueba.IBAN_LUIS;

import banco.service.BancoService;
import banco.dto.SolicitudTransferencia;

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
                    new SolicitudTransferencia(IBAN_ANA, IBAN_LUIS, new BigDecimal("60.00"), null), "empleado");
            BancoService banco = primera.getBean(BancoService.class);
            banco.iniciarRevision(banco.listarIncidencias().get(0).id(), "empleado");
        }
        // Abre de nuevo el archivo: Flyway ve las migraciones ya aplicadas y no toca nada.
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
            banco.resolverIncidencia(banco.listarIncidencias().get(0).id(), "Operación justificada.", "supervisor");
        }
        try (var tercera = arrancar(url)) {
            assertEquals("RESUELTA", tercera.getBean(BancoService.class).listarIncidencias().get(0).estado());
            var cambios = tercera.getBean(JdbcTemplate.class)
                    .queryForList("SELECT * FROM cambios_incidencia ORDER BY id");
            assertEquals(2, cambios.size());
            assertEquals("Operación justificada.", cambios.get(1).get("COMENTARIO"));
            // Auditoría: quién revisó y quién resolvió, también tras reiniciar.
            assertEquals("empleado", cambios.get(0).get("USUARIO"));
            assertEquals("supervisor", cambios.get(1).get("USUARIO"));
            org.junit.jupiter.api.Assertions.assertNotNull(cambios.get(1).get("FECHA"));
        }
    }

    private ConfigurableApplicationContext arrancar(String url) {
        return new SpringApplicationBuilder(Main.class)
                .web(WebApplicationType.NONE)
                .profiles("test") // datos de prueba; la URL de abajo sustituye a la de memoria
                .run("--spring.datasource.url=" + url, "--banco.deteccion.umbral-eur=50.00");
    }
}
