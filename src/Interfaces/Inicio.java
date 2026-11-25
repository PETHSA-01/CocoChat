package Interfaces;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Properties;

/**
 * Ventana de inicio de sesión de CocoChat.
 *
 * <p>Qué hace: muestra el saludo, una tarjeta con usuario y contraseña, el botón
 * «Acceder», un enlace para crear cuenta y un interruptor de tema claro/oscuro.</p>
 *
 * <p>Por qué está escrita a mano: el equipo necesita una identidad visual propia
 * (fuente TTF, colores e imágenes configurables) que el generador de formularios
 * de NetBeans no cubre sin código adicional. Aun así se usa {@code GroupLayout}
 * para que el posicionamiento siga siendo visible y editable desde la pestaña
 * Design del IDE.</p>
 *
 * <p>Toda la configuración visual vive en {@code src/Estilos.properties}: así
 * cualquier compañero puede cambiar colores, tamaños, tipografía o fondos sin
 * modificar este archivo Java.</p>
 *
 * @author pethsa
 */
public class Inicio extends JFrame {

    // ===================== Estilo =====================
    /**
     * Configuración visual leída una sola vez al cargar la clase. Es estática
     * para que la ventana y las clases internas de dibujo compartan los mismos
     * valores y no vuelvan a abrir el archivo en cada repintado.
     */
    private static final Properties P = new Properties();

    /** Estado del tema: lo alterna el botón luna/sol y lo consultan los métodos de estilo. */
    private static boolean oscuro = false;

    /** Fuente TTF registrada en el entorno gráfico; base de todos los tamaños de texto. */
    private static Font baseFuente;

    /**
     * Carga la configuración antes de que se construya cualquier componente.
     * Es necesario hacerlo en un bloque estático porque los métodos
     * {@code color()} y {@code fuente()} se llaman desde los constructores de
     * los panels, que se crean como campos de la clase.
     */
    static {
        try (InputStream in = Inicio.class.getResourceAsStream("/Estilos.properties")) {
            if (in != null) P.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        baseFuente = cargarFuente();
    }

    /**
     * Lee el TTF indicado en {@code fuente.archivo} y lo registra en el entorno
     * gráfico. Registrar la fuente es necesario para {@code deriveFont} trabaje
     * sobre una familia real aunque el sistema no tenga instalada «Satisfy».
     * Si el recurso falta o no es un TTF válido se usa Serif como respaldo, para
     * que la ventana siga siendo usable en lugar de fallar al arrancar.
     *
     * @return la fuente base lista para derivar los distintos tamaños
     */
    private static Font cargarFuente() {
        String ruta = P.getProperty("fuente.archivo", "/Interfaces/Elementos/Satisfy-Regular.ttf");
        try (InputStream in = Inicio.class.getResourceAsStream(ruta)) {
            if (in != null) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, in);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                return f;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Font(P.getProperty("fuente.nombre", "Serif"), Font.PLAIN, 16);
    }

    /**
     * Devuelve el prefijo de las claves según el tema activo («claro.» u
     * «oscuro.»). Centralizarlo evita repetir el mismo condicional en cada
     * consulta de color o de imagen de fondo.
     */
    static String prefijo() { return oscuro ? "oscuro." : "claro."; }

    /**
     * Invierte el tema claro/oscuro. Lo usan tanto Inicio como Registro para
     * compartir el mismo estado sin exponer el campo {@code oscuro}.
     */
    static void alternarTema() {
        oscuro = !oscuro;
    }

    /**
     * Convierte un valor hexadecimal del properties en un {@link Color} de Java.
     * Acepta tanto {@code #RRGGBB} como {@code #RRGGBBAA}: los dos dígitos extra
     * son la opacidad, necesaria para los campos del modo oscuro
     * ({@code #00000000}) que dejan ver la tarjeta a través de ellos.
     * El valor por defecto magenta ({@code #FF00FF}) es deliberado: si falta una
     * clave el error se ve a simple vista y no se confunde con un color del tema.
     *
     * @param clave nombre del color sin prefijo de tema, p. ej. {@code "texto"}
     * @return el color resuelto para el tema actual
     */
     static Color color(String clave) {
        String v = P.getProperty(prefijo() + clave, "#FF00FF").trim().substring(1);
        int r = Integer.parseInt(v.substring(0, 2), 16);
        int g = Integer.parseInt(v.substring(2, 4), 16);
        int b = Integer.parseInt(v.substring(4, 6), 16);
        int a = v.length() >= 8 ? Integer.parseInt(v.substring(6, 8), 16) : 255;
        return new Color(r, g, b, a);
    }

    /**
     * Devuelve la fuente base en el tamaño configurado para un rol (título,
     * etiqueta, texto, enlace o botón). Se usa {@code deriveFont} en lugar de
     * crear una fuente nueva para conservar la familia TTF en todos los textos.
     *
     * @param clave sufijo de la clave de tamaño, p. ej. {@code "titulo"}
     */
     static Font fuente(String clave) {
        int tam = Integer.parseInt(P.getProperty("fuente." + clave, "16"));
        return baseFuente.deriveFont(Font.PLAIN, (float) tam);
    }

    /**
     * Activa el antialiasing de figuras y texto. Es necesario porque todos los
     * componentes de esta ventana se dibujan a mano en {@code paintComponent};
     * sin esto los bordes redondeados y las letras se verían escalonados.
     */
     static void suavizar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // ===================== Componentes =====================
    /*
     * Se declaran como campos finales para que el constructor, los layouts y
     * aplicarTema() trabajen siempre sobre las mismas instancias al cambiar de tema.
     */
    private final PanelFondo fondo = new PanelFondo();
    private final PanelTarjeta tarjeta = new PanelTarjeta();
    private final JLabel lblSaludo = new JLabel("Hola de nuevo <3");
    private final JLabel lblTitulo = new JLabel("Inicio de Sesion", SwingConstants.CENTER);
    private final JLabel lblCuenta = new JLabel("Crearte una cuenta");
    private final JTextField txtUsuario = new JTextField();
    private final JTextField txtClave = new JTextField();
    private final CampoRedondo campoUsuario = new CampoRedondo("Usuario", txtUsuario);
    private final CampoRedondo campoClave = new CampoRedondo("Contraseña", txtClave);
    private final BotonRosa btnAcceder = new BotonRosa("Acceder");
    private final BotonTema btnTema = new BotonTema();

    /**
     * Construye la ventana. El orden es importante: primero se arman los
     * layouts (para que los panels tengan LayoutManager), después se aplica el
     * tema (que depende de esos panels y recarga la imagen) y al final se
     * sustituye el contentPane por el panel que pinta el fondo.
     */
    public Inicio() {
        super("Inicio de Sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);          // Al cerrar termina la aplicación, no queda oculta.
        setMinimumSize(new Dimension(900, 620));          // Evita que la tarjeta se deforme al encoger la ventana.
        setSize(1200, 700);                               // Tamaño inicial amplio para que el diseño respire.
        setLocationRelativeTo(null);                      // Centra la ventana en la pantalla.

        lblCuenta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Parece un enlace interactivo.
        lblCuenta.setHorizontalAlignment(SwingConstants.RIGHT);  
        lblCuenta.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { abrirRegistro(); }
        });
        // Queda a la derecha, bajo los campos.
        // Alternar el tema exige recargar la imagen y repintar; aplicarTema() hace ambas cosas.
        btnTema.addActionListener(e -> { alternarTema(); aplicarTema(); });
        btnAcceder.addActionListener(e -> abrirDashboard());   // <- nuevo

        armarTarjeta();
        armarFondo();
        aplicarTema();
        setContentPane(fondo);                            // El panel propio es el que dibuja el fondo con imagen.
    }

    // --------- Layout de la tarjeta ---------
    /**
     * Coloca título, campos, enlace y botón dentro de la tarjeta con GroupLayout.
     * Los márgenes y anchos son explícitos para que el contenido no invada el
     * borde redondeado que dibuja {@link PanelTarjeta}.
     */
    private void armarTarjeta() {
        GroupLayout gl = new GroupLayout(tarjeta);
        tarjeta.setLayout(gl);
        gl.setAutoCreateContainerGaps(false);

        gl.setHorizontalGroup(gl.createSequentialGroup()
            .addGap(52)
            .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                // Ancho común de 340 para todos los elementos: mantiene la tarjeta proporcionada.
                .addComponent(lblTitulo, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                .addComponent(campoUsuario, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                .addComponent(campoClave, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                .addComponent(lblCuenta, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                .addComponent(btnAcceder, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE))
            .addGap(52));

        gl.setVerticalGroup(gl.createSequentialGroup()
            .addGap(48)
            .addComponent(lblTitulo)
            .addGap(48)
            .addComponent(campoUsuario, 66, 66, 66)       // Alto fijo: el borde punteado se dibuja alrededor de este espacio.
            .addGap(20)
            .addComponent(campoClave, 66, 66, 66)
            .addGap(6)
            .addComponent(lblCuenta)
            .addGap(22)
            .addComponent(btnAcceder, 50, 50, 50)         // 50 px de alto dejan el texto centrado sin pegarse al borde.
            .addGap(56));
    }

    // --------- Layout general ---------
    /**
     * Organiza el contenido completo de la ventana: el botón de tema arriba a
     * la derecha y el saludo con la tarjeta en el centro. Se usa una segunda
     * capa ({@code centro}) transparente para agrupar saludo y tarjeta sin
     * tapar la imagen de fondo.
     */
    private void armarFondo() {
        JPanel centro = new JPanel();
        centro.setOpaque(false);                          // Deja ver el fondo a través del contenedor.
        GroupLayout cl = new GroupLayout(centro);
        centro.setLayout(cl);
        cl.setHorizontalGroup(cl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(lblSaludo)
            .addComponent(tarjeta, 450, 450, 450));       // Columna única: saludo y tarjeta alineados.
        cl.setVerticalGroup(cl.createSequentialGroup()
            .addComponent(lblSaludo)
            .addGap(4)                                    // Separador corto entre saludo y tarjeta.
            .addComponent(tarjeta, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));

        GroupLayout gl = new GroupLayout(fondo);
        fondo.setLayout(gl);
        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(gl.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE) // El espacio flexible empuja el botón a la derecha.
                .addComponent(btnTema, 44, 44, 44)
                .addGap(32))
            .addComponent(centro, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
        gl.setVerticalGroup(gl.createSequentialGroup()
            .addGap(24)
            .addComponent(btnTema, 44, 44, 44)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(centro, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
    }

    // --------- Aplicar tema claro/oscuro ---------
    /**
     * Actualiza todos los colores y fuentes cuando cambia el tema. Se llama en
     * el constructor y en cada clic del botón luna/sol. Además de los textos,
     * recarga la imagen de fondo porque claro y oscuro usan archivos distintos,
     * y fuerza un repintado porque los componentes se dibujan a mano.
     */
     void aplicarTema() {
        Color texto = color("texto");
        lblSaludo.setFont(fuente("titulo").deriveFont(30f));  // El saludo es algo más pequeño que el título de la tarjeta.
        lblSaludo.setForeground(texto);
        lblTitulo.setFont(fuente("titulo"));
        lblTitulo.setForeground(texto);
        lblCuenta.setFont(fuente("enlace"));
        lblCuenta.setForeground(texto);

        for (JTextField t : new JTextField[]{txtUsuario, txtClave}) {
            t.setOpaque(false);                                // CampoRedondo dibuja el relleno y el borde.
            t.setBorder(null);                                 // Evita el rectángulo gris del JTextField por defecto.
            t.setFont(fuente("texto"));
            t.setForeground(texto);
            t.setCaretColor(texto);                            // El cursor sigue siendo visible en tema oscuro.
        }
        btnAcceder.setFont(fuente("boton"));
        campoUsuario.setFont(fuente("etiqueta"));              // La etiqueta se pinta sobre el borde del campo.
        campoClave.setFont(fuente("etiqueta"));
        fondo.cargarImagen();                                  // Cada tema tiene su propia imagen de fondo.
        getContentPane().repaint();
        repaint();                                             // Obliga a redibujar las figuras personalizadas.
    }

    // ===================== Fondo con imagen al 35% =====================
    /**
     * Panel usado como contentPane: pinta primero un color base y después la
     * imagen del tema con la opacidad configurada. Hace falta un panel propio
     * porque un JFrame no permite ajustar por separado la transparencia de una
     * imagen de fondo.
     */
     static class PanelFondo extends JPanel {
        private BufferedImage img;

        PanelFondo() { cargarImagen(); }

        /**
         * Obtiene del properties la imagen correspondiente al tema activo. Se
         * vuelve a llamar desde aplicarTema() porque claro y oscuro usan rutas
         * distintas; si la lectura falla, {@code img} queda a null y solo se
         * pinta el color base.
         */
        void cargarImagen() {
            try {
                String ruta = P.getProperty(prefijo() + "fondo.imagen");
                img = ImageIO.read(Inicio.class.getResource(ruta));
            } catch (Exception e) {
                img = null;
            }
        }

        /**
         * Dibuja el fondo: color sólido y, encima, la imagen con
         * {@code AlphaComposite.SRC_OVER} para respetar {@code fondo.opacidad}.
         * El escalado tipo «cover» toma la mayor escala que cubre todo el panel
         * y centra el resultado, evitando franjas vacías al redimensionar.
         * Se dibuja sobre una copia del Graphics y se llama a {@code dispose()}
         * para no alterar el estado gráfico del contenedor padre.
         */
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(color("fondo.base"));
            g.fillRect(0, 0, getWidth(), getHeight());
            if (img != null) {
                float op = Float.parseFloat(P.getProperty("fondo.opacidad", "0.35"));
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, op));
                // escalado tipo "cover"
                double esc = Math.max((double) getWidth() / img.getWidth(),
                                      (double) getHeight() / img.getHeight());
                int w = (int) (img.getWidth() * esc), h = (int) (img.getHeight() * esc);
                g.drawImage(img, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            }
            g.dispose();
        }
    }

    // ===================== Tarjeta (borde + punteado) =====================
    /**
     * Tarjeta redondeada con borde sólido y un segundo trazo punteado interior.
     * Es transparente para que la imagen de fondo se vea alrededor y entre ambos
     * trazos; el aspecto se dibuja a mano en lugar de usar un borde estándar.
     */
     static class PanelTarjeta extends JPanel {
        PanelTarjeta() { setOpaque(false); }

        /**
         * Pinta la tarjeta con esquinas suaves (radio 70) y el borde punteado
         * interior. El patrón de guiones se consigue con un BasicStroke, sin
         * necesidad de imágenes ni de un LookAndFeel personalizado.
         */
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);
            int w = getWidth(), h = getHeight();

            RoundRectangle2D caja = new RoundRectangle2D.Double(1, 1, w - 3, h - 3, 70, 70);
            g.setColor(color("tarjeta.relleno"));
            g.fill(caja);
            g.setColor(color("tarjeta.borde"));
            g.setStroke(new BasicStroke(2f));
            g.draw(caja);

            g.setColor(color("tarjeta.punteado"));
            g.setStroke(new BasicStroke(14f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    10f, new float[]{96f, 14f}, 0f));
            g.draw(new RoundRectangle2D.Double(32, 32, w - 65, h - 65, 56, 56));
            g.dispose();
        }
    }

    // ===================== Campo redondeado con etiqueta sobre el borde =====================
    /**
     * Envuelve un JTextField para dibujar una caja redondeada con su etiqueta
     * («Usuario»/«Contraseña») apoyada sobre el trazo punteado. El campo real
     * queda transparente dentro: se sigue escribiendo con normalidad mientras
     * este panel controla toda la apariencia.
     */
     static class CampoRedondo extends JPanel {
        private final String etiqueta;

        /**
         * Coloca el JTextField con márgenes internos para que el texto no toque
         * el borde dibujado en paintComponent.
         */
        CampoRedondo(String etiqueta, JTextField campo) {
            this.etiqueta = etiqueta;
            setOpaque(false);
            GroupLayout gl = new GroupLayout(this);
            setLayout(gl);
            gl.setHorizontalGroup(gl.createSequentialGroup()
                .addGap(28)
                .addComponent(campo, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addGap(24));
            gl.setVerticalGroup(gl.createSequentialGroup()
                .addGap(30)
                .addComponent(campo, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        }

        /**
         * Dibuja el relleno redondeado, el borde punteado y la etiqueta. Restar
         * un rectángulo del borde con {@link Area} deja el hueco limpio donde se
         * escribe la etiqueta, para que el trazo no le pase por encima.
         */
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);
            int w = getWidth(), h = getHeight();
            int top = 18;                         // la caja empieza debajo de la etiqueta
            RoundRectangle2D caja = new RoundRectangle2D.Double(2, top, w - 5, h - top - 4, 80, 80);

            g.setColor(color("campo.relleno"));
            g.fill(caja);

            // Etiqueta y hueco en el borde punteado detrás del texto
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int tx = 24, ty = top + 2;
            Rectangle hueco = new Rectangle(tx - 4, top - 8, fm.stringWidth(etiqueta) + 8, 16);

            Area borde = new Area(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    10f, new float[]{26f, 10f}, 0f).createStrokedShape(caja));
            borde.subtract(new Area(hueco));
            g.setColor(color("campo.borde"));
            g.fill(borde);

            g.setColor(color("texto"));
            g.drawString(etiqueta, tx, ty);
            g.dispose();
        }
    }

    // ===================== Botón "Acceder" =====================
    /**
     * Botón dibujado a mano para aplicar el color del tema y un cambio de brillo
     * al pasar el ratón, sin depender del LookAndFeel del sistema.
     */
     static class BotonRosa extends JButton {
        /**
         * Desactiva el relleno, el borde y el foco del botón estándar porque
         * paintComponent se encarga de dibujarlos; el cursor de mano refuerza
         * que es interactivo.
         */
        BotonRosa(String texto) {
            super(texto);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        /**
         * Pinta la píldora redondeada y centra el texto. Usar
         * {@code getModel().isRollover()} da el efecto hover sin necesidad de
         * listeners adicionales.
         */
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);
            Color base = color("boton.relleno");
            g.setColor(getModel().isRollover() ? base.brighter() : base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.setColor(color("boton.texto"));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
     private void abrirRegistro() {
    Registro r = new Registro(this);
    r.setBounds(getBounds());
    r.setVisible(true);
    setVisible(false);
}
     private void abrirDashboard() {
    Amigos amigos = new Amigos();
    amigos.setBounds(getBounds());
    amigos.setExtendedState(getExtendedState());
    amigos.setVisible(true);
    dispose();
}

// Lo llama Registro al presionar la flecha de regreso
void volver(Rectangle bounds) {
    aplicarTema();          // por si cambió el modo claro/oscuro en Registro
    setBounds(bounds);
    setVisible(true);
}

    // ===================== Botón luna / sol =====================
    /**
     * Interruptor de tema: muestra una luna en modo claro y un sol en modo
     * oscuro para indicar al usuario qué tema obtendrá al hacer clic.
     */
     static class BotonTema extends JButton {
        /** Igual que BotonRosa: se dibuja a mano, por eso se desactiva el aspecto estándar. */
        BotonTema() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        /**
         * Dibuja el icono según el estado de {@code oscuro}: un sol (círculo más
         * ocho rayos) para volver al tema claro, o una luna recortando un círculo
         * con {@link Area} para pasar al oscuro.
         */
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
            g.setColor(color("icono"));

            if (oscuro) {                         // sol
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Double(cx - 7, cy - 7, 14, 14));
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    g.drawLine((int) (cx + Math.cos(a) * 11), (int) (cy + Math.sin(a) * 11),
                               (int) (cx + Math.cos(a) * 17), (int) (cy + Math.sin(a) * 17));
                }
            } else {                              // luna
                Area luna = new Area(new Ellipse2D.Double(cx - 14, cy - 14, 28, 28));
                luna.subtract(new Area(new Ellipse2D.Double(cx - 4, cy - 19, 26, 26)));
                g.fill(luna);
            }
            g.dispose();
        }
    }

    // ===================== main =====================
    /**
     * Punto de entrada para probar únicamente esta ventana (Run File en NetBeans).
     * {@code invokeLater} garantiza que Swing se construya en el EDT. En la
     * aplicación completa se usa
     * {@code arranques.ClaseNacaYEstupidaParaProbarInterfaces}, que además
     * levanta el servidor y la ventana de log antes de mostrar este inicio.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Inicio().setVisible(true));
    }
}
