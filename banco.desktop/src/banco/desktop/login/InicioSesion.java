package banco.desktop.login;

import java.util.concurrent.TimeUnit;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import banco.desktop.api.BancoApi;
import banco.desktop.api.Sesion;

// Se ejecuta al arrancar, antes de mostrar la ventana principal.
// Pide credenciales, las cambia por un token JWT y obtiene el rol; sin login válido no se entra.
// Es la clase de "ciclo de vida" de e4: se registra con -lifeCycleURI en banco.desktop.product.
public class InicioSesion {

    @PostContextCreate
    void login() {
        Display display = Display.getDefault();
        Shell shell = new Shell(display);
        String error = null;
        String usuarioPrevio = null;

        while (true) {
            LoginDialog dialogo = new LoginDialog(shell, error, usuarioPrevio);
            if (dialogo.open() != Window.OK) {
                // El usuario canceló: no se abre la aplicación.
                shell.dispose();
                System.exit(0);
            }

            try (BancoApi api = new BancoApi()) {
                // /api/login valida la contraseña (200/401) y devuelve el token.
                String token = api.login(dialogo.usuario(), dialogo.password()).get(10, TimeUnit.SECONDS);
                Sesion.iniciar(dialogo.usuario(), token);
                // Con el token ya guardado, /api/me devuelve el nombre y el rol.
                var yo = api.consultarYo().get(10, TimeUnit.SECONDS);
                Sesion.rol(yo.get("rol").getAsString());
                Sesion.nombre(yo.get("nombre").getAsString());
                break;
            } catch (Exception e) {
                Sesion.cerrar();
                error = mensajeDe(e);
                usuarioPrevio = dialogo.usuario();
            }
        }

        shell.dispose();
    }

    private static String mensajeDe(Exception e) {
        Throwable causa = e.getCause() != null ? e.getCause() : e;
        if (causa instanceof BancoApi.ErrorApi error && error.codigo() == 401) {
            return "Usuario o contraseña incorrectos.";
        }
        return "No se pudo conectar con el servidor. ¿Está arrancado el backend?";
    }
}
