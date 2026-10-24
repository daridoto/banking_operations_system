package banco;

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

    @PostMapping("/transferencias")
    public List<CuentaRespuesta> transferir(@Valid @RequestBody SolicitudTransferencia solicitud) {
        return banco.transferir(solicitud);
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
    public Incidencia iniciarRevision(@PathVariable("id") long id) {
        return banco.iniciarRevision(id);
    }

    @PostMapping("/incidencias/{id}/resolucion")
    public Incidencia resolverIncidencia(@PathVariable("id") long id,
                                        @Valid @RequestBody SolicitudResolucion solicitud) {
        return banco.resolverIncidencia(id, solicitud.comentario());
    }
}
