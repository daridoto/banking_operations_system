package banco.desktop.parts;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

// Tarjeta de indicador: un título pequeño en gris y un valor grande debajo.
// El borde redondeado se dibuja a mano (paint listener). Usa los colores del sistema
// para verse bien tanto en modo claro como en modo oscuro.
class Indicador extends Composite {
    private final Label valor;
    private final Color textoNormal;

    Indicador(Composite padre, String titulo) {
        super(padre, SWT.NONE);
        setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        textoNormal = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        setBackgroundMode(SWT.INHERIT_FORCE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 14;
        layout.marginHeight = 10;
        setLayout(layout);
        addPaintListener(evento -> {
            Rectangle area = getClientArea();
            evento.gc.setAntialias(SWT.ON);
            evento.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            evento.gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 10, 10);
        });

        Label etiqueta = new Label(this, SWT.NONE);
        etiqueta.setText(titulo);
        etiqueta.setForeground(Estilo.GRIS);

        valor = new Label(this, SWT.NONE);
        valor.setText("—");
        valor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Font grande = FontDescriptor.createFrom(valor.getFont()).setStyle(SWT.BOLD).increaseHeight(8)
                .createFont(getDisplay());
        valor.setFont(grande);
        valor.addDisposeListener(evento -> grande.dispose());
    }

    // color null = color de texto normal; un color solo para llamar la atención.
    void setValor(String texto, Color color) {
        valor.setText(texto);
        valor.setForeground(color == null ? textoNormal : color);
        layout();
    }
}
