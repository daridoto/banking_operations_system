package banco.model;

// Una fila de la tabla usuarios: un empleado del banco con su login, nombre y rol.
public record Usuario(String username, String passwordHash, String nombreCompleto, String rol,
                      boolean activo) {
}
