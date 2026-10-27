package banco.desktop;

import java.util.concurrent.TimeUnit;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import banco.desktop.api.BancoApi;
import banco.desktop.api.Sesion;

// Se ejecuta al arrancar, antes de mostrar la ventana principal.
// Pide credenciales y las valida contra el backend; sin login válido no se entra.
public class LifeCycle {

    @PostContextCreate
    void login() {
        Display display = Display.getDefault();
        Shell shell = new Shell(display);
        String error = null;

        while (true) {
            LoginDialog dialogo = new LoginDialog(shell, error);
            if (dialogo.open() != Window.OK) {
                // El usuario canceló: no se abre la aplicación.
                shell.dispose();
                System.exit(0);
            }

            Sesion.iniciar(dialogo.usuario(), dialogo.password());
            try (BancoApi api = new BancoApi()) {
                // Una petición real valida las credenciales: 200 vale, 401 no.
                api.listarCuentas().get(10, TimeUnit.SECONDS);
                break;
            } catch (Exception e) {
                Sesion.cerrar();
                error = mensajeDe(e);
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
