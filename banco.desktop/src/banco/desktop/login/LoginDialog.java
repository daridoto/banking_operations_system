package banco.desktop.login;

import banco.desktop.parts.Estilo;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

// Cuadro de inicio de sesión: pide usuario y contraseña. No habla con la API;
// solo recoge lo que el usuario escribe. La validación la hace InicioSesion.
// El diseño es propio (franja de marca, botón ancho), así que no usa la barra de
// botones estándar de JFace: el botón Entrar llama a okPressed() y Cancelar a cancelPressed().
class LoginDialog extends Dialog {
    // Incluye la barra de título de la ventana.
    private static final Point TAMANO = new Point(420, 610);
    private Text usuarioText;
    private Text passwordText;
    private String usuario;
    private String password = "";
    private final String error;

    // usuarioPrevio: lo que se escribió en el intento anterior, para no tener que repetirlo.
    LoginDialog(Shell parent, String error, String usuarioPrevio) {
        super(parent);
        this.error = error;
        this.usuario = usuarioPrevio == null ? "" : usuarioPrevio;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Countercheck — Iniciar sesión");
    }

    @Override
    protected Point getInitialSize() {
        return TAMANO;
    }

    // El diálogo se abre sobre una ventana invisible; se centra en la pantalla principal.
    @Override
    protected Point getInitialLocation(Point tamano) {
        Rectangle pantalla = getShell().getDisplay().getPrimaryMonitor().getBounds();
        return new Point(pantalla.x + (pantalla.width - tamano.x) / 2,
                pantalla.y + (pantalla.height - tamano.y) / 2);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = new Composite(parent, SWT.NONE);
        GridLayout sinMargen = new GridLayout(1, false);
        sinMargen.marginWidth = 0;
        sinMargen.marginHeight = 0;
        sinMargen.verticalSpacing = 0;
        area.setLayout(sinMargen);
        area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        crearFranjaMarca(area);
        crearFormulario(area);
        return area;
    }

    // Franja superior verde con el logo completo (ojo + COUNTERCHECK).
    private void crearFranjaMarca(Composite padre) {
        Composite franja = new Composite(padre, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 26;
        franja.setLayout(layout);
        franja.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        franja.setBackground(Estilo.VERDE_MARCA);
        franja.setBackgroundMode(SWT.INHERIT_FORCE);

        Label logo = new Label(franja, SWT.CENTER);
        logo.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
        Image imagen = ResourceLocator.imageDescriptorFromBundle("banco.desktop", "icons/logo-login.png")
                .map(descriptor -> descriptor.createImage())
                .orElse(null);
        if (imagen != null) {
            logo.setImage(imagen);
            logo.addDisposeListener(evento -> imagen.dispose());
        } else {
            logo.setText("COUNTERCHECK");
            logo.setForeground(Estilo.BLANCO);
        }
    }

    private void crearFormulario(Composite padre) {
        Composite formulario = new Composite(padre, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 32;
        layout.marginTop = 22;
        layout.marginBottom = 16;
        layout.verticalSpacing = 6;
        formulario.setLayout(layout);
        formulario.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label titulo = new Label(formulario, SWT.NONE);
        titulo.setText("Acceso de empleados");
        Font grande = FontDescriptor.createFrom(titulo.getFont()).setStyle(SWT.BOLD).increaseHeight(5)
                .createFont(padre.getDisplay());
        titulo.setFont(grande);
        titulo.addDisposeListener(evento -> grande.dispose());
        Label subtitulo = new Label(formulario, SWT.NONE);
        subtitulo.setText("Back office · Operaciones bancarias");
        subtitulo.setForeground(Estilo.GRIS);

        usuarioText = campo(formulario, "Usuario", SWT.NONE, 18);
        usuarioText.setText(usuario);
        passwordText = campo(formulario, "Contraseña", SWT.PASSWORD, 10);

        // Hueco reservado para el error, para que el diálogo no cambie de tamaño.
        Label aviso = new Label(formulario, SWT.WRAP);
        GridData espacioAviso = new GridData(SWT.FILL, SWT.TOP, true, false);
        espacioAviso.verticalIndent = 10;
        aviso.setLayoutData(espacioAviso);
        aviso.setForeground(Estilo.ROJO);
        aviso.setText(error == null ? "" : "⚠  " + error);

        Button entrar = new Button(formulario, SWT.PUSH);
        entrar.setText("Entrar");
        GridData anchoCompleto = new GridData(SWT.FILL, SWT.TOP, true, false);
        anchoCompleto.verticalIndent = 10;
        anchoCompleto.heightHint = 34;
        entrar.setLayoutData(anchoCompleto);
        entrar.addListener(SWT.Selection, evento -> okPressed());
        // Enter en cualquier campo pulsa Entrar; Esc cancela (lo hace Dialog por defecto).
        formulario.getShell().setDefaultButton(entrar);

        Button cancelar = new Button(formulario, SWT.PUSH);
        cancelar.setText("Cancelar");
        cancelar.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        cancelar.addListener(SWT.Selection, evento -> cancelPressed());

        Label pie = new Label(formulario, SWT.NONE);
        pie.setText("Countercheck v1.0 · Uso interno");
        pie.setForeground(Estilo.GRIS);
        pie.setLayoutData(new GridData(SWT.LEFT, SWT.END, true, true));

        // Si ya hay usuario (reintento), el cursor va directo a la contraseña.
        (usuario.isEmpty() ? usuarioText : passwordText).setFocus();
    }

    private static Text campo(Composite padre, String etiqueta, int estilo, int separacion) {
        Label texto = new Label(padre, SWT.NONE);
        texto.setText(etiqueta);
        GridData arriba = new GridData();
        arriba.verticalIndent = separacion;
        texto.setLayoutData(arriba);
        Text campo = new Text(padre, SWT.BORDER | estilo);
        GridData ancho = new GridData(SWT.FILL, SWT.CENTER, true, false);
        ancho.heightHint = 26;
        campo.setLayoutData(ancho);
        return campo;
    }

    // Sin barra de botones estándar: los botones están dentro del formulario.
    @Override
    protected Control createButtonBar(Composite parent) {
        Composite vacia = new Composite(parent, SWT.NONE);
        GridData oculta = new GridData();
        oculta.exclude = true;
        vacia.setLayoutData(oculta);
        return vacia;
    }

    @Override
    protected void okPressed() {
        usuario = usuarioText.getText().strip();
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
