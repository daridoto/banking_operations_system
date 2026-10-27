package banco.model;

// Una fila de la tabla usuarios: credenciales de login y rol.
public record Usuario(String username, String passwordHash, String rol, boolean activo) {
}
