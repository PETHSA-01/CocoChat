package Interfaces;

import static Interfaces.Inicio.color;
import static Interfaces.Inicio.fuente;
import static Interfaces.Inicio.suavizar;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * Ventana base con menú lateral, botón de tema y área de contenido con scroll.
 * Las subclases solo definen qué mostrar en crearContenido().
 */
public abstract class VentanaBase extends JFrame {

    public enum Seccion {
        AMIGOS("Amigos"), GRUPOS("Grupos"), DESCUBRIR("Descubrir");
        final String etiqueta;
        Seccion(String etiqueta) { this.etiqueta = etiqueta; }
    }

    public enum TipoAvatar { USUARIO, GRUPO }

    /** Datos mínimos de una card. Se llena desde la base de datos. */
    public record ItemLista(String nombre, boolean enLinea) {}

    /** Nombre que se muestra en el menú lateral (el usuario con sesión iniciada). */
    protected static String nombreUsuario = "Tú";

    private static final int COLUMNAS = 3;
    private static final int ALTO_CARD = 150;

    private final Seccion actual;
    private final PanelPrincipal raiz = new PanelPrincipal();
    private final PanelLateral lateral = new PanelLateral();
    private final AvatarComp avatarPropio = new AvatarComp(TipoAvatar.USUARIO);
    private final JLabel lblUsuario = new JLabel(nombreUsuario, SwingConstants.CENTER);
    private final Map<Seccion, JLabel> navs = new EnumMap<>(Seccion.class);
    private final Inicio.BotonTema btnTema = new Inicio.BotonTema();
    private final BotonSalir btnSalir = new BotonSalir();
    private final JScrollPane scroll = new JScrollPane();

    protected VentanaBase(Seccion actual) {
        super("Chat - " + actual.etiqueta);
        this.actual = actual;

        for (Seccion s : Seccion.values()) {
            JLabel l = new JLabel(s.etiqueta);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { ir(s); }
            });
            navs.put(s, l);
        }
        btnTema.addActionListener(e -> { Inicio.alternarTema(); aplicarTema(); });
        // Cerrar sesión vuelve a la pantalla de acceso y libera esta ventana.
        btnSalir.addActionListener(e -> cerrarSesion());
    }

    // ===================== Lo que implementan las subclases =====================

    /** Devuelve el contenido de la zona derecha (secciones con cards o mensaje vacío). */
    protected abstract JComponent crearContenido();

    /** Se llama al hacer clic en una card. Por defecto no hace nada. */
    protected void alSeleccionar(ItemLista item) { }

    // ===================== Construcción =====================

    /** Las subclases lo llaman al final de su constructor, cuando ya tienen sus datos. */
    protected final void iniciar() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1200, 700);
        setLocationRelativeTo(null);

        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        // El primero agregado queda encima: el botón de tema flota sobre el contenido.
        raiz.add(btnTema);
        raiz.add(scroll);
        raiz.add(lateral);

        armarLateral();
        armarRaiz();
        setContentPane(raiz);
        aplicarTema();
    }

    private void armarLateral() {
        GroupLayout gl = new GroupLayout(lateral);
        lateral.setLayout(gl);

        GroupLayout.ParallelGroup menu = gl.createParallelGroup(GroupLayout.Alignment.LEADING);
        for (Seccion s : Seccion.values()) menu.addComponent(navs.get(s));
        // El logout comparte la columna de los enlaces: queda dentro del menú lateral.
        menu.addComponent(btnSalir, 44, 44, 44);

        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(gl.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(avatarPropio, 134, 134, 134)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(lblUsuario, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(gl.createSequentialGroup().addGap(28).addGroup(menu)));

        gl.setVerticalGroup(gl.createSequentialGroup()
            .addGap(64)
            .addComponent(avatarPropio, 134, 134, 134)
            .addGap(14)
            .addComponent(lblUsuario)
            .addGap(64)
            .addComponent(navs.get(Seccion.AMIGOS))
            .addGap(10)
            .addComponent(navs.get(Seccion.GRUPOS))
            .addGap(10)
            .addComponent(navs.get(Seccion.DESCUBRIR))
            .addGap(0, 0, Short.MAX_VALUE)      // El espacio sobrante deja el logout abajo del menú.
            .addComponent(btnSalir, 44, 44, 44)
            .addGap(24));
    }

    private void armarRaiz() {
        GroupLayout gl = new GroupLayout(raiz);
        raiz.setLayout(gl);

        gl.setHorizontalGroup(gl.createSequentialGroup()
            .addComponent(lateral, 290, 290, 290)
            .addGap(70)
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scroll, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(gl.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(btnTema, 44, 44, 44)
                    .addGap(32))));

        gl.setVerticalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(lateral, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(scroll, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(gl.createSequentialGroup()
                .addGap(24)
                .addComponent(btnTema, 44, 44, 44)));
    }

    protected final void aplicarTema() {
        lblUsuario.setFont(fuente("seccion"));
        lblUsuario.setForeground(color("usuario.texto"));
        navs.forEach((s, l) -> {
            l.setFont(fuente("nav"));
            l.setForeground(color(s == actual ? "nav.activo" : "nav.inactivo"));
        });
        reconstruir();
        raiz.repaint();
    }

    /** Vuelve a generar el contenido. Llámalo cada vez que cambien los datos. */
    protected final void reconstruir() {
        scroll.setViewportView(crearContenido());
        scroll.getViewport().setOpaque(false);
        scroll.revalidate();
        scroll.repaint();
    }

    private void ir(Seccion destino) {
        if (destino == actual) return;
        VentanaBase v = switch (destino) {
            case AMIGOS -> new Amigos();
            case GRUPOS -> new Grupo();
            case DESCUBRIR -> new Descubrir();
        };
        v.setBounds(getBounds());
        v.setExtendedState(getExtendedState());
        v.setVisible(true);
        dispose();
    }

    /**
     * Vuelve al inicio de sesión. Se crea una instancia nueva de Inicio para
     * que el formulario de acceso quede limpio; el tema se mantiene porque
     * {@code oscuro} es estático y {@code Inicio} lo aplica al construirse.
     */
    private void cerrarSesion() {
        Inicio inicio = new Inicio();
        inicio.setBounds(getBounds());
        inicio.setExtendedState(getExtendedState());
        inicio.setVisible(true);
        dispose();
    }

    // ===================== Constructores de contenido =====================

    /** Contenido típico de Amigos / Descubrir: secciones "En línea" y "Desconectados". */
    protected final JComponent contenidoUsuarios(List<ItemLista> usuarios, String textoVacio) {
        if (usuarios.isEmpty()) return mensajeVacio(textoVacio);

        List<ItemLista> conectados = new ArrayList<>();
        List<ItemLista> desconectados = new ArrayList<>();
        for (ItemLista u : usuarios) (u.enLinea() ? conectados : desconectados).add(u);

        List<JComponent> secciones = new ArrayList<>();
        if (!conectados.isEmpty())
            secciones.add(seccion("En línea", color("seccion.activa"), conectados, TipoAvatar.USUARIO, true));
        if (!desconectados.isEmpty())
            secciones.add(seccion("Desconectados", color("seccion.inactiva"), desconectados, TipoAvatar.USUARIO, true));
        return apilar(secciones);
    }

    /** Título + cuadrícula de cards de 3 columnas. */
    protected final JComponent seccion(String titulo, Color colorTitulo, List<ItemLista> items,
                                       TipoAvatar tipo, boolean conEstado) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(fuente("seccion"));
        lbl.setForeground(colorTitulo);
        JComponent cuadricula = cuadricula(items, tipo, conEstado);

        GroupLayout gl = new GroupLayout(p);
        p.setLayout(gl);
        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(lbl)
            .addComponent(cuadricula, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        gl.setVerticalGroup(gl.createSequentialGroup()
            .addComponent(lbl)
            .addGap(16)
            .addComponent(cuadricula, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
        return p;
    }

    private JComponent cuadricula(List<ItemLista> items, TipoAvatar tipo, boolean conEstado) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        GroupLayout gl = new GroupLayout(p);
        p.setLayout(gl);

        int filas = (items.size() + COLUMNAS - 1) / COLUMNAS;
        GroupLayout.ParallelGroup[] columnas = new GroupLayout.ParallelGroup[COLUMNAS];
        for (int c = 0; c < COLUMNAS; c++) columnas[c] = gl.createParallelGroup(GroupLayout.Alignment.LEADING);

        GroupLayout.SequentialGroup vertical = gl.createSequentialGroup();
        List<Component> todas = new ArrayList<>();

        for (int f = 0; f < filas; f++) {
            GroupLayout.ParallelGroup fila = gl.createParallelGroup(GroupLayout.Alignment.LEADING);
            for (int c = 0; c < COLUMNAS; c++) {
                int i = f * COLUMNAS + c;
                JComponent celda;
                if (i < items.size()) {
                    celda = new Tarjeta(items.get(i), tipo, conEstado, this::alSeleccionar);
                } else {                        // hueco para mantener el ancho de las columnas
                    celda = new JPanel();
                    ((JPanel) celda).setOpaque(false);
                }
                todas.add(celda);
                columnas[c].addComponent(celda, 200, 330, Short.MAX_VALUE);
                fila.addComponent(celda, ALTO_CARD, ALTO_CARD, ALTO_CARD);
            }
            vertical.addGroup(fila);
            if (f < filas - 1) vertical.addGap(30);
        }

        GroupLayout.SequentialGroup horizontal = gl.createSequentialGroup();
        for (int c = 0; c < COLUMNAS; c++) {
            horizontal.addGroup(columnas[c]);
            if (c < COLUMNAS - 1) horizontal.addGap(30);
        }
        gl.setHorizontalGroup(horizontal);
        gl.setVerticalGroup(vertical);
        gl.linkSize(SwingConstants.HORIZONTAL, todas.toArray(new Component[0]));
        return p;
    }

    /** Apila varias secciones una debajo de otra. */
    protected final JComponent apilar(List<JComponent> secciones) {
        PanelScroll p = new PanelScroll(false);
        p.setBorder(BorderFactory.createEmptyBorder(24, 0, 30, 40));
        GroupLayout gl = new GroupLayout(p);
        p.setLayout(gl);

        GroupLayout.ParallelGroup h = gl.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup v = gl.createSequentialGroup();
        for (int i = 0; i < secciones.size(); i++) {
            JComponent s = secciones.get(i);
            h.addComponent(s, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
            v.addComponent(s, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
            if (i < secciones.size() - 1) v.addGap(40);
        }
        gl.setHorizontalGroup(h);
        gl.setVerticalGroup(v);
        return p;
    }

    /** Mensaje centrado cuando no hay cards. */
    protected final JComponent mensajeVacio(String texto) {
        PanelScroll p = new PanelScroll(true);
        JLabel l = new JLabel(texto, SwingConstants.CENTER);
        l.setFont(fuente("seccion"));
        l.setForeground(color("texto"));

        GroupLayout gl = new GroupLayout(p);
        p.setLayout(gl);
        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addComponent(l, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        gl.setVerticalGroup(gl.createSequentialGroup()
            .addGap(0, 0, Short.MAX_VALUE)
            .addComponent(l)
            .addGap(0, 0, Short.MAX_VALUE));
        return p;
    }

    // ===================== Dibujo del avatar =====================

    private static void dibujarAvatar(Graphics2D g, TipoAvatar tipo, double x, double y, double d) {
        Ellipse2D circulo = new Ellipse2D.Double(x, y, d, d);
        g.setColor(color("avatar.fondo"));
        g.fill(circulo);

        Shape clipAnterior = g.getClip();
        g.setClip(circulo);
        double cx = x + d / 2;
        if (tipo == TipoAvatar.USUARIO) {
            persona(g, cx, y + 0.36 * d, 0.17 * d, 0.62 * d, null);
        } else {
            Color sep = color("avatar.fondo");
            persona(g, cx - 0.25 * d, y + 0.36 * d, 0.10 * d, 0.34 * d, null);
            persona(g, cx + 0.25 * d, y + 0.36 * d, 0.10 * d, 0.34 * d, null);
            persona(g, cx, y + 0.42 * d, 0.13 * d, 0.46 * d, sep);
        }
        g.setClip(clipAnterior);

        g.setColor(color("avatar.borde"));
        g.setStroke(new BasicStroke(Math.max(2f, (float) d / 40f)));
        g.draw(circulo);
    }

    private static void persona(Graphics2D g, double cx, double cyCabeza, double r, double ancho, Color separador) {
        Ellipse2D cabeza = new Ellipse2D.Double(cx - r, cyCabeza - r, 2 * r, 2 * r);
        Ellipse2D cuerpo = new Ellipse2D.Double(cx - ancho / 2, cyCabeza + r * 1.3, ancho, ancho * 1.1);
        if (separador != null) {                 // halo para separar figuras que se superponen
            g.setColor(separador);
            g.setStroke(new BasicStroke(6f));
            g.draw(cabeza);
            g.draw(cuerpo);
        }
        g.setColor(color("avatar.figura"));
        g.fill(cabeza);
        g.fill(cuerpo);
    }

    // ===================== Componentes internos =====================

    /**
     * Botón de cerrar sesión dibujado a mano: solo pinta el glifo con el color
     * de texto de los botones del tema. No lleva relleno para no introducir el
     * rosa de boton.relleno dentro del menú lateral.
     */
    private static class BotonSalir extends JButton {
        BotonSalir() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Cerrar sesión");
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);

            Color tinta = color("boton.texto");
            g.setColor(getModel().isRollover() ? tinta.brighter() : tinta);
            double s = Math.min(getWidth(), getHeight()) / 44.0;
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
            g.setStroke(new BasicStroke(3f * (float) s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Marco de puerta abierto por la derecha y flecha hacia fuera.
            g.drawLine((int) (cx - 15 * s), (int) (cy - 12 * s), (int) (cx - 5 * s), (int) (cy - 12 * s));
            g.drawLine((int) (cx - 15 * s), (int) (cy - 12 * s), (int) (cx - 15 * s), (int) (cy + 12 * s));
            g.drawLine((int) (cx - 15 * s), (int) (cy + 12 * s), (int) (cx - 5 * s), (int) (cy + 12 * s));

            g.drawLine((int) (cx - 6 * s), (int) cy, (int) (cx + 15 * s), (int) cy);
            g.drawLine((int) (cx + 7 * s), (int) (cy - 7 * s), (int) (cx + 15 * s), (int) cy);
            g.drawLine((int) (cx + 7 * s), (int) (cy + 7 * s), (int) (cx + 15 * s), (int) cy);

            g.dispose();
        }
    }

    private static class PanelPrincipal extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            g.setColor(color("principal.fondo"));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private static class PanelLateral extends JPanel {
        PanelLateral() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);
            // Esquinas izquierdas fuera de pantalla: solo se redondea el lado derecho.
            RoundRectangle2D r = new RoundRectangle2D.Double(-80, 0, getWidth() + 80 - 1, getHeight() - 1, 80, 80);
            g.setColor(color("lateral.relleno"));
            g.fill(r);
            g.setColor(color("lateral.borde"));
            g.setStroke(new BasicStroke(2f));
            g.draw(r);
            g.dispose();
        }
    }

    private static class AvatarComp extends JPanel {
        private final TipoAvatar tipo;

        AvatarComp(TipoAvatar tipo) { this.tipo = tipo; setOpaque(false); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);
            int d = Math.min(getWidth(), getHeight()) - 4;
            dibujarAvatar(g, tipo, (getWidth() - d) / 2.0, (getHeight() - d) / 2.0, d);
            g.dispose();
        }
    }

    /** Card de usuario o grupo. */
    private static class Tarjeta extends JPanel {
        private final ItemLista item;
        private final TipoAvatar tipo;
        private final boolean conEstado;

        Tarjeta(ItemLista item, TipoAvatar tipo, boolean conEstado, Consumer<ItemLista> alClic) {
            this.item = item;
            this.tipo = tipo;
            this.conEstado = conEstado;
            setOpaque(false);
            setPreferredSize(new Dimension(330, ALTO_CARD));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { alClic.accept(item); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);
            int w = getWidth(), h = getHeight();

            RoundRectangle2D caja = new RoundRectangle2D.Double(1, 1, w - 3, h - 3, 90, 90);
            g.setColor(color("card.relleno"));
            g.fill(caja);
            g.setColor(color(tipo == TipoAvatar.GRUPO ? "card.borde.grupo" : "card.borde"));
            g.setStroke(new BasicStroke(2f));
            g.draw(caja);

            // Avatar (un poco más abajo si hay estado arriba)
            int d = 78;
            int ay = (h - d) / 2 + (conEstado ? 12 : 0);
            dibujarAvatar(g, tipo, 34, ay, d);
            int cy = ay + d / 2;

            // Nombre
            g.setColor(color("card.texto"));
            g.setFont(fuente("card"));
            FontMetrics fm = g.getFontMetrics();
            int xNombre = 34 + d + 30;
            String nombre = recortar(item.nombre(), fm, w - xNombre - 24);
            g.drawString(nombre, xNombre, cy + (fm.getAscent() - fm.getDescent()) / 2);

            // Estado: texto + punto arriba a la derecha
            if (conEstado) {
                int dot = 28;
                int dx = w - 52, dy = 19;
                g.setColor(color(item.enLinea() ? "estado.enlinea" : "estado.desconectado"));
                g.fillOval(dx, dy, dot, dot);

                String texto = item.enLinea() ? "En Línea" : "Desconectado";
                g.setColor(color("card.texto"));
                g.setFont(fuente("estado"));
                FontMetrics fe = g.getFontMetrics();
                g.drawString(texto, dx - 10 - fe.stringWidth(texto),
                        dy + dot / 2 + (fe.getAscent() - fe.getDescent()) / 2);
            }
            g.dispose();
        }

        private static String recortar(String t, FontMetrics fm, int max) {
            if (fm.stringWidth(t) <= max) return t;
            while (t.length() > 1 && fm.stringWidth(t + "…") > max) t = t.substring(0, t.length() - 1);
            return t + "…";
        }
    }

    /** Panel que se ajusta al ancho del scroll (y opcionalmente al alto, para centrar el mensaje vacío). */
    private static class PanelScroll extends JPanel implements Scrollable {
        private final boolean ajustarAlto;

        PanelScroll(boolean ajustarAlto) { this.ajustarAlto = ajustarAlto; setOpaque(false); }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return ajustarAlto; }
    }
}