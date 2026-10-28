package banco;

import static banco.DatosPrueba.IBAN_ANA;
import static banco.DatosPrueba.IBAN_LUIS;

import banco.dto.SolicitudTransferencia;
import banco.service.BancoService;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Buscador y ficha de cliente. Datos de prueba: Ana (DNI, cliente 1) y Luis (NIE, cliente 2).
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "empleado")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientesApiTest {
    @Autowired private MockMvc api;
    @Autowired private BancoService banco;

    @Test
    void sinTextoDevuelveTodosOrdenadosPorNombre() throws Exception {
        api.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Ana García López"))
                .andExpect(jsonPath("$[1].nombre").value("Luis Martín Pérez"));
    }

    @Test
    void buscaPorNombreSinDistinguirMayusculasYPorDocumento() throws Exception {
        api.perform(get("/api/clientes").param("q", "GARCÍA"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].documento").value("12345678Z"));
        api.perform(get("/api/clientes").param("q", "x123"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Luis Martín Pérez"));
    }

    @Test
    void losComodinesDeLikeSeBuscanComoTexto() throws Exception {
        // Sin escapar, "%" encajaría con todos los clientes.
        api.perform(get("/api/clientes").param("q", "%"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void fichaConDatosYCuentas() throws Exception {
        api.perform(get("/api/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana García López"))
                .andExpect(jsonPath("$.tipo").value("FISICA"))
                .andExpect(jsonPath("$.tipoDocumento").value("DNI"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.fechaAlta").isNotEmpty())
                .andExpect(jsonPath("$.cuentas.length()").value(1))
                .andExpect(jsonPath("$.cuentas[0].iban").value(IBAN_ANA))
                .andExpect(jsonPath("$.cuentas[0].saldo").value(100))
                .andExpect(jsonPath("$.movimientos.length()").value(0))
                .andExpect(jsonPath("$.incidencias.length()").value(0));
        api.perform(get("/api/clientes/2")).andExpect(jsonPath("$.tipoDocumento").value("NIE"));
    }

    @Test
    void movimientosEIncidenciasVistosDesdeCadaCliente() throws Exception {
        banco.transferir(new SolicitudTransferencia(IBAN_ANA, IBAN_LUIS, new BigDecimal("60.00"),
                "Alquiler noviembre 2026"), "empleado");
        long incidencia = banco.listarIncidencias().get(0).id();
        banco.iniciarRevision(incidencia, "empleado");
        banco.resolverIncidencia(incidencia, "Pago de alquiler habitual.", "supervisor");

        // Para Ana el dinero sale; la otra parte es Luis.
        api.perform(get("/api/clientes/1"))
                .andExpect(jsonPath("$.cuentas[0].saldo").value(40))
                .andExpect(jsonPath("$.movimientos.length()").value(1))
                .andExpect(jsonPath("$.movimientos[0].sentido").value("SALIDA"))
                .andExpect(jsonPath("$.movimientos[0].ibanPropio").value(IBAN_ANA))
                .andExpect(jsonPath("$.movimientos[0].contraparte").value("Luis Martín Pérez"))
                .andExpect(jsonPath("$.movimientos[0].concepto").value("Alquiler noviembre 2026"))
                .andExpect(jsonPath("$.incidencias.length()").value(1))
                .andExpect(jsonPath("$.incidencias[0].estado").value("RESUELTA"))
                .andExpect(jsonPath("$.incidencias[0].resueltaPor").value("supervisor"));

        // Para Luis, la misma transferencia es una entrada que viene de Ana.
        api.perform(get("/api/clientes/2"))
                .andExpect(jsonPath("$.movimientos[0].sentido").value("ENTRADA"))
                .andExpect(jsonPath("$.movimientos[0].contraparte").value("Ana García López"))
                .andExpect(jsonPath("$.movimientos[0].ibanContraparte").value(IBAN_ANA));
    }

    @Test
    void clienteInexistenteDevuelve404() throws Exception {
        api.perform(get("/api/clientes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe el cliente 999."));
    }
}
