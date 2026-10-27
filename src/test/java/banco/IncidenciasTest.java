package banco;

import banco.service.BancoService;
import banco.dto.SolicitudTransferencia;
import banco.model.Incidencia;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "banco.deteccion.umbral-eur=50.00")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IncidenciasTest {
    @Autowired private BancoService banco;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc api;

    @Test
    void transferenciaElevadaSeRealizaYGeneraIncidenciaConsultable() throws Exception {
        api.perform(post("/api/transferencias").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"cuentaOrigenId":1,"cuentaDestinoId":2,"importe":60.00}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saldo").value(40))
                .andExpect(jsonPath("$[1].saldo").value(110));
        long idTransferencia = banco.listarTransferencias().get(0).id();
        api.perform(get("/api/incidencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transferenciaId").value(idTransferencia))
                .andExpect(jsonPath("$[0].regla").value("IMPORTE_ELEVADO"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].prioridad").value("MEDIA"))
                .andExpect(jsonPath("$[0].motivo").value(
                        "Importe 60.00 EUR superior al umbral de 50.00 EUR. Requiere revisión."))
                .andExpect(jsonPath("$[0].fecha").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"20.00", "50.00"})
    void noGeneraIncidenciaPorDebajoONivelDelUmbral(String importe) {
        banco.transferir(new SolicitudTransferencia(1L, 2L, new BigDecimal(importe)));
        assertEquals(1, banco.listarTransferencias().size());
        assertTrue(banco.listarIncidencias().isEmpty());
    }

    @Test
    void consultaUnaIncidenciaPorSuId() throws Exception {
        banco.transferir(new SolicitudTransferencia(1L, 2L, new BigDecimal("60.00")));
        banco.transferir(new SolicitudTransferencia(2L, 1L, new BigDecimal("70.00")));
        Incidencia incidencia = banco.listarIncidencias().get(1);

        api.perform(get("/api/incidencias/{id}", incidencia.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidencia.id()))
                .andExpect(jsonPath("$.transferenciaId").value(incidencia.transferenciaId()))
                .andExpect(jsonPath("$.regla").value(incidencia.regla()))
                .andExpect(jsonPath("$.motivo").value(incidencia.motivo()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.prioridad").value("MEDIA"))
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    void incidenciaInexistenteDevuelve404() throws Exception {
        api.perform(get("/api/incidencias/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe la incidencia 999."));
    }

    @Test
    void generaIncidenciaUnCentimoPorEncimaDelUmbral() {
        banco.transferir(new SolicitudTransferencia(1L, 2L, new BigDecimal("50.01")));
        assertEquals(1, banco.listarIncidencias().size());
    }

    @Test
    void transferenciaRechazadaNoGeneraIncidencia() {
        assertThrows(IllegalArgumentException.class, () -> banco.transferir(
                new SolicitudTransferencia(1L, 2L, new BigDecimal("101.00"))));
        comprobarSinCambios();
    }

    @Test
    void falloAlGuardarIncidenciaRevierteSaldosYTransferencia() {
        // Provoca un fallo real en la última escritura SQL, en una base aislada.
        jdbc.execute("ALTER TABLE incidencias ADD CONSTRAINT fallo_incidencia CHECK (prioridad <> 'MEDIA')");
        assertThrows(DataAccessException.class, () -> banco.transferir(
                new SolicitudTransferencia(1L, 2L, new BigDecimal("60.00"))));
        comprobarSinCambios();
    }

    private void comprobarSinCambios() {
        assertEquals(new BigDecimal("100.00"), banco.listarCuentas().get(0).saldo());
        assertEquals(new BigDecimal("50.00"), banco.listarCuentas().get(1).saldo());
        assertTrue(banco.listarTransferencias().isEmpty());
        assertTrue(banco.listarIncidencias().isEmpty());
    }

    @Test
    void iniciaRevisionYRegistraElCambioFechado() throws Exception {
        long id = crearIncidencia();
        api.perform(post("/api/incidencias/{id}/revision", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        api.perform(get("/api/incidencias/{id}", id))
                .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        var cambio = jdbc.queryForMap("SELECT * FROM cambios_incidencia WHERE incidencia_id = ?", id);
        assertEquals("PENDIENTE", cambio.get("ESTADO_ANTERIOR"));
        assertEquals("EN_REVISION", cambio.get("ESTADO_NUEVO"));
        assertNotNull(cambio.get("FECHA"));

        api.perform(post("/api/incidencias/{id}/revision", id))
                .andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }

    @Test
    void noIniciaRevisionDeIncidenciaInexistente() throws Exception {
        api.perform(post("/api/incidencias/999/revision"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe la incidencia 999."));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }

    @Test
    void noReabreUnaIncidenciaResuelta() throws Exception {
        long id = crearIncidencia();
        jdbc.update("UPDATE incidencias SET estado = 'RESUELTA' WHERE id = ?", id);
        api.perform(post("/api/incidencias/{id}/revision", id))
                .andExpect(status().isConflict());
        assertEquals("RESUELTA", banco.consultarIncidencia(id).estado());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }

    @Test
    void falloDelHistorialRevierteCambioDeEstado() {
        long id = crearIncidencia();
        jdbc.execute("ALTER TABLE cambios_incidencia ADD CONSTRAINT fallo_cambio CHECK (estado_nuevo <> 'EN_REVISION')");
        assertThrows(DataAccessException.class, () -> banco.iniciarRevision(id));
        assertEquals("PENDIENTE", banco.consultarIncidencia(id).estado());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }

    private long crearIncidencia() {
        banco.transferir(new SolicitudTransferencia(1L, 2L, new BigDecimal("60.00")));
        return banco.listarIncidencias().get(0).id();
    }

    @Test
    void consultaHistorialOrdenadoSoloDeLaIncidenciaIndicada() throws Exception {
        long id = crearIncidencia();
        banco.iniciarRevision(id);
        banco.resolverIncidencia(id, "Operación justificada.");
        banco.transferir(new SolicitudTransferencia(2L, 1L, new BigDecimal("70.00")));
        banco.iniciarRevision(banco.listarIncidencias().get(1).id());

        api.perform(get("/api/incidencias/{id}/historial", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].incidenciaId").value(id))
                .andExpect(jsonPath("$[0].estadoAnterior").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].estadoNuevo").value("EN_REVISION"))
                .andExpect(jsonPath("$[0].fecha").isNotEmpty())
                .andExpect(jsonPath("$[0].comentario").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].incidenciaId").value(id))
                .andExpect(jsonPath("$[1].estadoAnterior").value("EN_REVISION"))
                .andExpect(jsonPath("$[1].estadoNuevo").value("RESUELTA"))
                .andExpect(jsonPath("$[1].fecha").isNotEmpty())
                .andExpect(jsonPath("$[1].comentario").value("Operación justificada."));
    }

    @Test
    void historialVacioSiNoHayCambios() throws Exception {
        long id = crearIncidencia();
        api.perform(get("/api/incidencias/{id}/historial", id))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void historialDeIncidenciaInexistenteDevuelve404() throws Exception {
        api.perform(get("/api/incidencias/999/historial"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe la incidencia 999."));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void resuelveConComentarioYConservaHistorial() throws Exception {
        long id = crearIncidencia();
        banco.iniciarRevision(id);
        api.perform(post("/api/incidencias/{id}/resolucion", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comentario\":\"Operación justificada.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTA"));
        assertEquals("RESUELTA", banco.consultarIncidencia(id).estado());
        var cambios = jdbc.queryForList("SELECT * FROM cambios_incidencia WHERE incidencia_id = ? ORDER BY id", id);
        assertEquals(2, cambios.size());
        assertEquals("EN_REVISION", cambios.get(1).get("ESTADO_ANTERIOR"));
        assertEquals("RESUELTA", cambios.get(1).get("ESTADO_NUEVO"));
        assertEquals("Operación justificada.", cambios.get(1).get("COMENTARIO"));
        assertNotNull(cambios.get(1).get("FECHA"));
        api.perform(post("/api/incidencias/{id}/resolucion", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"comentario\":\"Otro cierre\"}"))
                .andExpect(status().isConflict());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"comentario\":null}", "{\"comentario\":\"\"}", "{\"comentario\":\"   \"}"})
    @WithMockUser(roles = "SUPERVISOR")
    void rechazaResolucionSinComentario(String cuerpo) throws Exception {
        long id = crearIncidencia();
        banco.iniciarRevision(id);
        api.perform(post("/api/incidencias/{id}/resolucion", id)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Indica un comentario de resolución de entre 1 y 1000 caracteres, no solo espacios."));
        assertEquals("EN_REVISION", banco.consultarIncidencia(id).estado());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void limitaLongitudDelComentario() throws Exception {
        long id = crearIncidencia();
        banco.iniciarRevision(id);
        api.perform(post("/api/incidencias/{id}/resolucion", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"comentario\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
        api.perform(post("/api/incidencias/{id}/resolucion", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"comentario\":\"" + "a".repeat(1000) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    void empleadoNoPuedeResolver() throws Exception {
        long id = crearIncidencia();
        banco.iniciarRevision(id);
        api.perform(post("/api/incidencias/{id}/resolucion", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"comentario\":\"Operación justificada.\"}"))
                .andExpect(status().isForbidden());
        assertEquals("EN_REVISION", banco.consultarIncidencia(id).estado());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void noResuelvePendienteNiInexistente() throws Exception {
        long id = crearIncidencia();
        api.perform(post("/api/incidencias/{id}/resolucion", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"comentario\":\"Revisada\"}"))
                .andExpect(status().isConflict());
        api.perform(post("/api/incidencias/999/resolucion")
                .contentType(MediaType.APPLICATION_JSON).content("{\"comentario\":\"Revisada\"}"))
                .andExpect(status().isNotFound());
        assertEquals("PENDIENTE", banco.consultarIncidencia(id).estado());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }

    @Test
    void falloDelHistorialRevierteResolucion() {
        long id = crearIncidencia();
        banco.iniciarRevision(id);
        jdbc.execute("ALTER TABLE cambios_incidencia ADD CONSTRAINT fallo_resolucion CHECK (estado_nuevo <> 'RESUELTA')");
        assertThrows(DataAccessException.class, () -> banco.resolverIncidencia(id, "Revisada"));
        assertEquals("EN_REVISION", banco.consultarIncidencia(id).estado());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cambios_incidencia", Integer.class));
    }
}
