package com.chat.cliente;

import com.chat.common.Peticion;
import com.chat.common.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * El "teléfono" del cliente: se conecta al servidor, envía peticiones
 * y tiene un hilo que escucha TODO lo que llega (respuestas y eventos).
 *
 * Uso desde las pantallas:
 *
 *   ConexionServidor conexion = new ConexionServidor();
 *   conexion.setEscucha(new ConexionServidor.Escucha() {
 *       public void alRecibir(Peticion p) {
 *           // OJO: esto corre en el hilo de escucha, NO en el de Swing.
 *           // Para tocar la pantalla usen SwingUtilities.invokeLater(() -> { ... });
 *       }
 *       public void alDesconectar() { ... }
 *   });
 *   conexion.conectar("192.168.1.50", 5000);
 *   conexion.enviar(Protocolo.LOGIN, "ana", "cacahuate");
 *
 * Debe haber UNA sola conexión por cliente, compartida por todas las pantallas.
 */
public class ConexionServidor {

    /** Lo que se ejecuta cuando llega algo del servidor o se pierde la conexión. */
    public interface Escucha {
        void alRecibir(Peticion peticion);
        void alDesconectar();
    }

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Thread hiloEscucha;
    private volatile Escucha escucha;

    /** Abre el socket y arranca el hilo que escucha al servidor. */
    public void conectar(String host, int puerto) throws IOException {
        socket = new Socket(host, puerto);
        // UTF-8 en ambos lados (el servidor debe usar lo mismo) para que acentos y ñ lleguen bien.
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        hiloEscucha = new Thread(this::escuchar, "escucha-servidor");
        hiloEscucha.setDaemon(true); // se cierra solo al cerrar la aplicación
        hiloEscucha.start();
    }

    /** Quién recibe lo que llega del servidor (normalmente la pantalla activa o un controlador). */
    public void setEscucha(Escucha escucha) {
        this.escucha = escucha;
    }

    /** Envía una petición ya armada. Ejemplo: enviar("LOGIN|ana|cacahuate"). */
    public synchronized void enviar(String cadena) {
        if (!estaConectado()) {
            throw new IllegalStateException("No hay conexión con el servidor");
        }
        salida.println(cadena);
    }

    /** Atajo: arma y envía. Ejemplo: enviar(Protocolo.LOGIN, "ana", "cacahuate"). */
    public void enviar(String accion, String... campos) {
        enviar(Protocolo.armar(accion, campos));
    }

    public boolean estaConectado() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /** Cierra la conexión (por ejemplo al hacer logout o cerrar la ventana). */
    public void cerrar() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
    }

    /** Lo que hace el hilo de escucha: leer línea por línea hasta que se cierre la conexión. */
    private void escuchar() {
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.isEmpty()) continue;
                Peticion peticion;
                try {
                    peticion = Protocolo.leer(linea);
                } catch (IllegalArgumentException e) {
                    System.err.println("Línea inválida del servidor: " + linea);
                    continue;
                }
                Escucha e = escucha;
                if (e != null) e.alRecibir(peticion);
            }
        } catch (IOException e) {
            // El socket se cerró o se cayó el servidor.
        } finally {
            cerrar();
            Escucha e = escucha;
            if (e != null) e.alDesconectar();
        }
    }
}
