package banco.desktop.parts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

// "Foto de perfil" genérica de la ficha: silueta de persona o, si el cliente es una
// empresa, de un edificio. Se dibuja con código (GC), así que no necesita imágenes
// y se ve nítida a cualquier tamaño.
public class Avatar extends Canvas {
    private static final int TAMANO = 96;
    private static final Color FONDO = new Color(229, 232, 236);
    private static final Color SILUETA = new Color(158, 165, 174);
    private static final Color VENTANA = new Color(229, 232, 236);
    private boolean empresa;

    public Avatar(Composite padre) {
        super(padre, SWT.DOUBLE_BUFFERED);
        addPaintListener(evento -> dibujar(evento.gc));
    }

    public void setEmpresa(boolean empresa) {
        this.empresa = empresa;
        redraw();
    }

    @Override
    public Point computeSize(int anchoSugerido, int altoSugerido, boolean cambiado) {
        return new Point(TAMANO, TAMANO);
    }

    private void dibujar(GC gc) {
        gc.setAntialias(SWT.ON);
        // Recuadro de fondo con esquinas redondeadas; todo lo demás se recorta dentro.
        gc.setBackground(FONDO);
        gc.fillRoundRectangle(0, 0, TAMANO, TAMANO, 16, 16);
        gc.setBackground(SILUETA);
        if (empresa) {
            dibujarEdificio(gc);
        } else {
            dibujarPersona(gc);
        }
    }

    // Cabeza: un círculo. Hombros: un óvalo ancho cuya mitad inferior queda fuera
    // del lienzo, así que solo se ve la parte de arriba. No llega a las esquinas,
    // por eso no estropea el redondeado del fondo.
    private void dibujarPersona(GC gc) {
        gc.fillOval(30, 14, 36, 36);
        gc.fillOval(14, 56, 68, 70);
    }

    // Bloque del edificio con tres filas de ventanas y una puerta.
    private void dibujarEdificio(GC gc) {
        gc.fillRectangle(26, 16, 44, 68);
        gc.setBackground(VENTANA);
        for (int fila = 0; fila < 3; fila++) {
            for (int columna = 0; columna < 2; columna++) {
                gc.fillRectangle(34 + columna * 18, 24 + fila * 16, 10, 9);
            }
        }
        gc.fillRectangle(42, 70, 12, 14);
    }
}
