package banco.desktop;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

// Cuadro de inicio de sesión: pide usuario y contraseña. No habla con la API;
// solo recoge lo que el usuario escribe. La validación la hace LifeCycle.
class LoginDialog extends Dialog {
    private Text usuarioText;
    private Text passwordText;
    private String usuario = "";
    private String password = "";
    private final String error;

    LoginDialog(Shell parent, String error) {
        super(parent);
        this.error = error;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Countercheck — Iniciar sesión");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite campos = new Composite(area, SWT.NONE);
        campos.setLayout(new GridLayout(2, false));
        campos.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        if (error != null) {
            Label aviso = new Label(campos, SWT.WRAP);
            GridData ancho = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
            aviso.setLayoutData(ancho);
            aviso.setText(error);
        }

        new Label(campos, SWT.NONE).setText("Usuario:");
        usuarioText = new Text(campos, SWT.BORDER);
        usuarioText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(campos, SWT.NONE).setText("Contraseña:");
        passwordText = new Text(campos, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        return area;
    }

    @Override
    protected void okPressed() {
        usuario = usuarioText.getText();
        password = passwordText.getText();
        super.okPressed();
    }

    String usuario() {
        return usuario;
    }

    String password() {
        return password;
    }
}
