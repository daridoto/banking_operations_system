package banco.web;

import banco.service.BancoService;
import banco.dto.CuentaRespuesta;
import banco.dto.ResumenOperativo;
import banco.dto.SolicitudTransferencia;
import banco.dto.SolicitudResolucion;
import banco.model.Transferencia;
import banco.model.Incidencia;
import banco.model.CambioIncidencia;

import java.security.Principal;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BancoController {
    private final BancoService banco;

    public BancoController(BancoService banco) {
        this.banco = banco;
    }

    @GetMapping("/cuentas")
    public List<CuentaRespuesta> listarCuentas() {
        return banco.listarCuentas();
    }

    @GetMapping("/resumen")
    public ResumenOperativo resumen() {
        return banco.resumen();
    }

    @PostMapping("/transferencias")
    public List<CuentaRespuesta> transferir(@Valid @RequestBody SolicitudTransferencia solicitud,
                                            Principal usuario) {
        return banco.transferir(solicitud, usuario.getName());
    }

    @GetMapping("/transferencias")
    public List<Transferencia> listarTransferencias() {
        return banco.listarTransferencias();
    }

    @GetMapping("/incidencias")
    public List<Incidencia> listarIncidencias() {
        return banco.listarIncidencias();
    }

    @GetMapping("/incidencias/{id}")
    public Incidencia consultarIncidencia(@PathVariable("id") long id) {
        return banco.consultarIncidencia(id);
    }

    @GetMapping("/incidencias/{id}/historial")
    public List<CambioIncidencia> consultarHistorialIncidencia(@PathVariable("id") long id) {
        return banco.consultarHistorialIncidencia(id);
    }

    @PostMapping("/incidencias/{id}/revision")
    public Incidencia iniciarRevision(@PathVariable("id") long id, Principal usuario) {
        return banco.iniciarRevision(id, usuario.getName());
    }

    @PostMapping("/incidencias/{id}/resolucion")
    public Incidencia resolverIncidencia(@PathVariable("id") long id,
                                        @Valid @RequestBody SolicitudResolucion solicitud,
                                        Principal usuario) {
        return banco.resolverIncidencia(id, solicitud.comentario(), usuario.getName());
    }
}
