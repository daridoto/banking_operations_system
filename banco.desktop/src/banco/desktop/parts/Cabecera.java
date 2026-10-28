package banco.desktop.parts;

import banco.desktop.api.Sesion;
import jakarta.annotation.PostConstruct;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

// Barra superior de la ventana: marca a la izquierda y empleado conectado a la derecha.
// Se registra en Application.e4xmi como ToolControl de la barra superior (trim bar).
public class Cabecera {
    private static final Color VERDE_MARCA = Estilo.VERDE_MARCA;
    private static final Color MENTA = Estilo.MENTA;
    private static final Color BLANCO = Estilo.BLANCO;

    @PostConstruct
    public void crear(Composite parent) {
        Composite barra = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(4, false);
        layout.marginWidth = 14;
        layout.marginHeight = 6;
        layout.horizontalSpacing = 10;
        barra.setLayout(layout);
        barra.setBackground(VERDE_MARCA);
        // Los hijos heredan el fondo verde en vez de pintar el gris por defecto.
        barra.setBackgroundMode(SWT.INHERIT_FORCE);

        // El ojo del logo; logo-ojo@2x.png se usa solo en pantallas Retina.
        Image logo = ResourceLocator.imageDescriptorFromBundle("banco.desktop", "icons/logo-ojo.png")
                .map(descriptor -> descriptor.createImage())
                .orElse(null);
        Label ojo = new Label(barra, SWT.NONE);
        if (logo != null) {
            ojo.setImage(logo);
            ojo.addDisposeListener(evento -> logo.dispose());
        }

        Label marca = new Label(barra, SWT.NONE);
        marca.setText("COUNTERCHECK");
        marca.setForeground(BLANCO);
        Font negrita = FontDescriptor.createFrom(marca.getFont()).setStyle(SWT.BOLD).increaseHeight(3)
                .createFont(parent.getDisplay());
        marca.setFont(negrita);
        marca.addDisposeListener(evento -> negrita.dispose());

        Label seccion = new Label(barra, SWT.NONE);
        seccion.setText("·  Back office");
        seccion.setForeground(MENTA);
        seccion.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        // Quién está conectado: nombre completo (de /api/me) y rol.
        Label empleado = new Label(barra, SWT.NONE);
        empleado.setText(Sesion.nombre() + "  ·  " + Sesion.rol());
        empleado.setForeground(BLANCO);
        empleado.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    }
}
