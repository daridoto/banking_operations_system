package banco.dto;

// Una fila del buscador de clientes: lo justo para reconocerlo y abrir su ficha.
// nombre es el nombre completo o la razón social, según el tipo de cliente.
public record ClienteResumen(long id, String tipo, String nombre, String documento, String estado) {
}
