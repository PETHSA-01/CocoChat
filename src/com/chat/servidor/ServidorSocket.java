package com.chat.servidor;

import com.chat.servidor.bd.ConexionBD;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

/**
 * El servidor central: acepta conexiones y crea UN hilo por cliente.
 *
 * Todas las acciones pasan por aqui: los clientes nunca se hablan entre si,
 * le mandan todo al servidor y el servidor lo redirige (ver ClientesConectados).
 * Acepta n clientes al mismo tiempo (un hilo para cada uno).
 *
 * Para arrancarlo: ejecutar esta clase (tiene main). Opcionalmente se puede
 * pasar el puerto como argumento; si no, usa el 5000.
 */
public class ServidorSocket {

    public static final int PUERTO_DEFECTO = 5000;

    private final int puerto;
    private final RegistroLog log;
    private final ClientesConectados clientes = new ClientesConectados();
    private final Despachador despachador;
    private final List<ManejadorCliente> conexiones = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private Thread hiloAceptar;
    private volatile boolean encendido;

    public ServidorSocket(int puerto, RegistroLog log) {
        this.puerto = puerto;
        this.log = log;
        this.despachador = new Despachador(log);
    }

    /** Aqui es donde cada equipo registra su manejador (ManejadorAuth, ManejadorChats, ...). */
    public Despachador getDespachador() {
        return despachador;
    }

    public ClientesConectados getClientes() {
        return clientes;
    }

    public int getPuerto() {
        return puerto;
    }

    // =====================================================================
    //  Encender y apagar
    // =====================================================================

    /** Abre el puerto y empieza a aceptar clientes (no bloquea: el aceptar corre en su hilo). */
    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(puerto);
        encendido = true;

        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            ip = "desconocida";
        }
        log.registrar("Servidor iniciado. Esperando clientes en " + ip + ":" + puerto);

        hiloAceptar = new Thread(new Runnable() {
            @Override
            public void run() {
                aceptarClientes();
            }
        }, "servidor-aceptar");
        hiloAceptar.start();
    }

    /** Cierra el puerto y desconecta a todos los clientes. */
    public void detener() {
        encendido = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
        for (ManejadorCliente conexion : conexiones) {
            conexion.cerrar();
        }
        log.registrar("Servidor detenido");
    }

    /** Bucle del hilo principal del servidor: aceptar, crear hilo, volver a aceptar. */
    private void aceptarClientes() {
        int contador = 0;
        while (encendido) {
            try {
                Socket socket = serverSocket.accept();
                contador++;

                final ManejadorCliente manejador = new ManejadorCliente(socket, despachador, clientes, log);
                conexiones.add(manejador);

                Thread hilo = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            manejador.run();
                        } finally {
                            conexiones.remove(manejador);
                        }
                    }
                }, "cliente-" + contador);
                hilo.start();
            } catch (SocketException e) {
                // Se cerro el ServerSocket (detener()): salimos del bucle.
                break;
            } catch (IOException e) {
                log.registrar("Error al aceptar un cliente: " + e.getMessage());
            }
        }
    }

    // =====================================================================
    //  Arranque: ventana de log + servidor
    // =====================================================================

    public static void main(String[] args) {
        final int puerto = leerPuerto(args);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VentanaLog ventana = new VentanaLog();
                ventana.setVisible(true);

                ServidorSocket servidor = new ServidorSocket(puerto, ventana);

                // Cada equipo agrega aqui SU manejador cuando ya lo tenga:
                ConexionBD.inicializar(ventana);                                // equipo 1: crea BD y tablas
                servidor.getDespachador().registrar(new ManejadorAuth());       // equipo 1
                // servidor.getDespachador().registrar(new ManejadorChats());    // equipo 2
                // servidor.getDespachador().registrar(new ManejadorGrupos());   // equipo 3

                try {
                    servidor.iniciar();
                } catch (IOException e) {
                    ventana.registrar("No se pudo abrir el puerto " + puerto + ": " + e.getMessage());
                }
            }
        });
    }

    private static int leerPuerto(String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Puerto invalido, se usa el " + PUERTO_DEFECTO);
            }
        }
        return PUERTO_DEFECTO;
    }
}
