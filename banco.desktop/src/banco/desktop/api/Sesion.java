package banco.desktop.api;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// Credenciales del usuario que ha iniciado sesión, compartidas por toda la app.
// Se guardan solo en memoria mientras el cliente está abierto.
public final class Sesion {
    private static String usuario;
    private static String password;
    private static String rol;

    private Sesion() {
    }

    public static void iniciar(String usuario, String password) {
        Sesion.usuario = usuario;
        Sesion.password = password;
    }

    public static void cerrar() {
        usuario = null;
        password = null;
        rol = null;
    }

    public static boolean hayCredenciales() {
        return usuario != null && password != null;
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

    // Cabecera Authorization para Basic: "usuario:password" codificado en Base64.
    // Es lo mismo que hace la opción -u de curl.
    public static String cabeceraBasic() {
        String credenciales = usuario + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }
}
