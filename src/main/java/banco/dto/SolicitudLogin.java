package banco.dto;

// Credenciales que el cliente envía para iniciar sesión y obtener un token.
public record SolicitudLogin(String usuario, String password) {
}
