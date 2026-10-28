package banco.web;

import banco.dto.ClienteResumen;
import banco.dto.FichaCliente;
import banco.service.ClienteService;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Clientes del banco, para el buscador y la ficha del cliente RCP.
// El cliente se pide por su número interno, no por DNI: un documento de identidad
// en la URL acabaría en los registros de servidores y proxies.
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final ClienteService clientes;

    public ClienteController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    public List<ClienteResumen> buscar(@RequestParam(name = "q", defaultValue = "") String texto) {
        return clientes.buscar(texto);
    }

    @GetMapping("/{id}")
    public FichaCliente ficha(@PathVariable("id") long id) {
        return clientes.ficha(id);
    }
}
