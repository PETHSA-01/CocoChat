package Interfaces;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import static Interfaces.Inicio.color;
import static Interfaces.Inicio.fuente;
import static Interfaces.Inicio.suavizar;

public class Registro extends JFrame {

    private final Inicio inicio;

    private final Inicio.PanelFondo fondo = new Inicio.PanelFondo();
    private final Inicio.PanelTarjeta tarjeta = new Inicio.PanelTarjeta();
    private final JLabel lblTitulo = new JLabel("Registro", SwingConstants.CENTER);
    private final JTextField txtUsuario = new JTextField();
    private final JTextField txtClave = new JTextField();
    private final Inicio.CampoRedondo campoUsuario = new Inicio.CampoRedondo("Usuario", txtUsuario);
    private final Inicio.CampoRedondo campoClave = new Inicio.CampoRedondo("Contraseña", txtClave);
    private final Inicio.BotonRosa btnRegistrar = new Inicio.BotonRosa("Registrarse");
    private final Inicio.BotonTema btnTema = new Inicio.BotonTema();
    private final BotonAtras btnAtras = new BotonAtras();

    public Registro(Inicio inicio) {
        super("Registro");
        this.inicio = inicio;
        // Esta es una ventana secundaria: cerrarla con la X debe volver a Inicio,
        // no terminar la aplicación. volver() ya restaura y repinta Inicio.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { volver(); }
        });
        setMinimumSize(new Dimension(900, 620));
        setSize(1200, 700);
        setLocationRelativeTo(null);

        btnTema.addActionListener(e -> { Inicio.alternarTema(); aplicarTema(); });
        btnAtras.addActionListener(e -> volver());
        btnRegistrar.addActionListener(e -> registrar());

        armarTarjeta();
        armarFondo();
        aplicarTema();
        setContentPane(fondo);
    }

    // --------- Acciones ---------
    private void volver() {
        inicio.volver(getBounds());
        dispose();
    }

    private void registrar() {
        String usuario = txtUsuario.getText().trim();
        String clave = txtClave.getText().trim();
        if (usuario.isEmpty() || clave.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completa todos los campos.",
                    "Registro", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // TODO: enviar la petición de registro al servidor (ConexionServidor / Peticion)
        JOptionPane.showMessageDialog(this, "Cuenta creada para: " + usuario,
                "Registro", JOptionPane.INFORMATION_MESSAGE);
        volver();
    }

    // --------- Layout de la tarjeta ---------
    private void armarTarjeta() {
        GroupLayout gl = new GroupLayout(tarjeta);
        tarjeta.setLayout(gl);
        gl.setAutoCreateContainerGaps(false);

        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.TRAILING)
            .addGroup(gl.createSequentialGroup()
                .addGap(52)
                .addGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitulo, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                    .addComponent(campoUsuario, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                    .addComponent(campoClave, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                    .addComponent(btnRegistrar, GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE))
                .addGap(52))
            .addGroup(gl.createSequentialGroup()
                .addComponent(btnAtras, 36, 36, 36)
                .addGap(30)));

        gl.setVerticalGroup(gl.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(gl.createSequentialGroup()
                .addGap(48)
                .addComponent(lblTitulo)
                .addGap(48)
                .addComponent(campoUsuario, 66, 66, 66)
                .addGap(20)
                .addComponent(campoClave, 66, 66, 66)
                .addGap(56)
                .addComponent(btnRegistrar, 50, 50, 50)
                .addGap(56))
            .addGroup(gl.createSequentialGroup()
                .addGap(36)
                .addComponent(btnAtras, 44, 44, 44)));
    }

    // --------- Layout general ---------
    private void armarFondo() {
        GroupLayout gl = new GroupLayout(fondo);
        fondo.setLayout(gl);
        gl.setHorizontalGroup(gl.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(gl.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnTema, 44, 44, 44)
                .addGap(32))
            .addComponent(tarjeta, 450, 450, 450));
        gl.setVerticalGroup(gl.createSequentialGroup()
            .addGap(24)
            .addComponent(btnTema, 44, 44, 44)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(44) // equivale al saludo "Hola de nuevo" de Inicio, para que la tarjeta quede en el mismo lugar
            .addComponent(tarjeta, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
    }

    // --------- Aplicar tema claro/oscuro ---------
    private void aplicarTema() {
        Color texto = color("texto");
        lblTitulo.setFont(fuente("titulo"));
        lblTitulo.setForeground(texto);

        for (JTextField t : new JTextField[]{txtUsuario, txtClave}) {
            t.setOpaque(false);
            t.setBorder(null);
            t.setFont(fuente("texto"));
            t.setForeground(texto);
            t.setCaretColor(texto);
        }
        btnRegistrar.setFont(fuente("boton"));
        campoUsuario.setFont(fuente("etiqueta"));
        campoClave.setFont(fuente("etiqueta"));
        fondo.cargarImagen();
        getContentPane().repaint();
        repaint();
    }

    // ===================== Flecha de regreso "<" =====================
    private static class BotonAtras extends JButton {
        BotonAtras() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suavizar(g);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            g.setColor(color("icono"));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawPolyline(new int[]{cx + 5, cx - 5, cx + 5},
                           new int[]{cy - 12, cy, cy + 12}, 3);
            g.dispose();
        }
    }
}