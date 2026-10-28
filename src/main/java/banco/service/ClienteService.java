package banco.service;

import banco.dto.ClienteResumen;
import banco.dto.FichaCliente;
import banco.repository.ClienteRepository;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository clientes;

    public ClienteService(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    // Sin texto de búsqueda, devuelve todos los clientes.
    @Transactional(readOnly = true)
    public List<ClienteResumen> buscar(String texto) {
        return clientes.buscar(texto == null ? "" : texto.strip());
    }

    // La ficha hace varias consultas (datos, cuentas, movimientos, incidencias);
    // la transacción hace que todas lean el mismo estado de la base.
    @Transactional(readOnly = true)
    public FichaCliente ficha(long id) {
        return clientes.ficha(id);
    }
}
