package banco.desktop.api;

// Datos del usuario que ha iniciado sesión, compartidos por toda la app.
// Se guarda el token JWT, no la contraseña: esta solo viaja una vez, en el login.
// Todo vive solo en memoria mientras el cliente está abierto.
public final class Sesion {
    private static String usuario;
    private static String token;
    private static String rol;

    private Sesion() {
    }

    public static void iniciar(String usuario, String token) {
        Sesion.usuario = usuario;
        Sesion.token = token;
    }

    public static void cerrar() {
        usuario = null;
        token = null;
        rol = null;
    }

    public static boolean hayToken() {
        return token != null;
    }

    public static String usuario() {
        return usuario;
    }

    public static void rol(String rol) {
        Sesion.rol = rol;
    }

    public static String rol() {
        return rol;
    }

    // Cabecera Authorization para JWT: el token se envía tal cual, sin codificar.
    public static String cabeceraBearer() {
        return "Bearer " + token;
    }
}
