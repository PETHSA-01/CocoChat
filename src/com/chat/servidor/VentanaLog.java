package com.chat.servidor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Ventana del servidor donde se escribe TODO lo que pasa: cada peticion
 * recibida, de quien, para quien y su contenido.
 *
 * Muestra contrasenas y mensajes en texto visible. Es solo para que el
 * profesor revise durante la prueba; en un sistema real NUNCA se hace.
 */
public class VentanaLog extends JFrame implements RegistroLog {

    /** Si el texto pasa de este tamano, se borra lo mas viejo para no llenar la memoria. */
    private static final int MAXIMO_CARACTERES = 300000;

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea areaTexto;
    private final JCheckBox seguirAlFinal;

    public VentanaLog() {
        super("Servidor de chat - Registro de actividad");

        areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        areaTexto.setLineWrap(true);
        areaTexto.setWrapStyleWord(true);
        areaTexto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaTexto.setBackground(new Color(30, 30, 30));
        areaTexto.setForeground(new Color(220, 220, 220));
        areaTexto.setCaretColor(Color.WHITE);

        seguirAlFinal = new JCheckBox("Seguir al final", true);

        JButton botonLimpiar = new JButton("Limpiar");
        botonLimpiar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                areaTexto.setText("");
            }
        });

        JPanel barra = new JPanel();
        barra.add(seguirAlFinal);
        barra.add(botonLimpiar);

        setLayout(new BorderLayout());
        add(new JScrollPane(areaTexto), BorderLayout.CENTER);
        add(barra, BorderLayout.SOUTH);

        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /**
     * Escribe una linea con la hora. Se puede llamar desde cualquier hilo:
     * el cambio en la pantalla se hace en el hilo de Swing.
     */
    @Override
    public void registrar(final String mensaje) {
        final String linea = "[" + LocalTime.now().format(HORA) + "] " + mensaje + "\n";
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                areaTexto.append(linea);

                int exceso = areaTexto.getDocument().getLength() - MAXIMO_CARACTERES;
                if (exceso > 0) {
                    areaTexto.replaceRange("", 0, exceso);
                }
                if (seguirAlFinal.isSelected()) {
                    areaTexto.setCaretPosition(areaTexto.getDocument().getLength());
                }
            }
        });
    }
}
