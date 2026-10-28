package banco.desktop.api;

// Una fila del buscador de clientes (GET /api/clientes).
public record ClienteVista(long id, String tipo, String nombre, String documento, String estado) {
}
