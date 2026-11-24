package com.chat.servidor;

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
 * Un hilo por cliente: atiende a UNA persona conectada.
 *
 * Lee sus lineas, las anota en el log y se las pasa al Despachador.
 * Tambien es la "bocina" para hablarle a ese cliente: enviar(...).
 *
 * Los manejadores de los equipos reciben este objeto en cada peticion y lo
 * usan para responder (enviar) y para marcar el inicio / cierre de sesion.
 */
public class ManejadorCliente implements Runnable {

    private final Socket socket;
    private final Despachador despachador;
    private final ClientesConectados clientes;
    private final RegistroLog log;
    private final String direccion;

    /** Candado solo para iniciar / cerrar sesion (no se mezcla con el de enviar). */
    private final Object candadoSesion = new Object();

    private volatile PrintWriter salida;
    private volatile String usuario;
    private volatile boolean cerrado;

    public ManejadorCliente(Socket socket, Despachador despachador,
                            ClientesConectados clientes, RegistroLog log) {
        this.socket = socket;
        this.despachador = despachador;
        this.clientes = clientes;
        this.log = log;
        this.direccion = String.valueOf(socket.getRemoteSocketAddress());
    }

    // =====================================================================
    //  El hilo: leer lineas hasta que el cliente se vaya
    // =====================================================================

    @Override
    public void run() {
        try {
            // UTF-8 en ambos lados (igual que ConexionServidor) para que acentos y n lleguen bien.
            BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            log.registrar("Se conecto un cliente desde " + direccion);

            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.isEmpty()) {
                    continue;
                }
                procesar(linea);
            }
        } catch (IOException e) {
            // El cliente cerro la conexion o se cayo la red: es normal, se limpia abajo.
        } finally {
            cerrarSesion();
            cerrar();
            log.registrar("Se desconecto el cliente " + direccion);
        }
    }

    /** Lee una linea, la anota y se la entrega al Despachador. */
    private void procesar(String linea) {
        Peticion peticion;
        try {
            peticion = Protocolo.leer(linea);
        } catch (IllegalArgumentException e) {
            log.registrar(quien() + " mando una linea invalida: " + linea);
            enviar(Protocolo.error("DESCONOCIDO", "Linea invalida"));
            return;
        }
        log.registrar(quien() + " -> " + despachador.describir(peticion));
        despachador.despachar(peticion, this);
    }

    // =====================================================================
    //  Hablarle al cliente
    // =====================================================================

    /**
     * Envia una cadena ya armada (usen Protocolo.ok / error / evento / armar).
     * Es segura si varios hilos le escriben al mismo cliente a la vez.
     */
    public synchronized void enviar(String cadena) {
        PrintWriter escritor = salida;
        if (escritor == null || cerrado) {
            return;
        }
        escritor.println(cadena);
        log.registrar("Envie a " + quien() + ": " + cadena);
    }

    // =====================================================================
    //  Sesion: quien es este cliente
    // =====================================================================

    /** true si este cliente ya inicio sesion. */
    public boolean estaAutenticado() {
        return usuario != null;
    }

    /** El usuario de esta conexion, o null si todavia no inicia sesion. */
    public String getUsuario() {
        return usuario;
    }

    /**
     * Marca que este cliente ya inicio sesion como "nombre" y avisa a los demas
     * con EVENTO|ESTADO|nombre|1.
     *
     * Debe llamarla ManejadorAuth cuando el LOGIN sea correcto (antes de responder OK).
     * Devuelve false si ese usuario ya esta conectado desde otra conexion.
     */
    public boolean iniciarSesion(String nombre) {
        synchronized (candadoSesion) {
            if (nombre.equals(usuario)) {
                return true;
            }
            if (usuario != null) {
                cerrarSesion();
            }
            if (!clientes.registrar(nombre, this)) {
                return false;
            }
            usuario = nombre;
            clientes.difundir(Protocolo.evento(Protocolo.ESTADO, nombre, Protocolo.CONECTADO), nombre);
            return true;
        }
    }

    /**
     * Cierra la sesion (LOGOUT o conexion caida) y avisa a los demas con
     * EVENTO|ESTADO|nombre|0. Si no habia sesion, no hace nada.
     * Tambien se llama sola cuando el cliente se desconecta.
     */
    public void cerrarSesion() {
        synchronized (candadoSesion) {
            String nombre = usuario;
            if (nombre == null) {
                return;
            }
            usuario = null;
            clientes.quitar(nombre, this);
            clientes.difundir(Protocolo.evento(Protocolo.ESTADO, nombre, Protocolo.DESCONECTADO), nombre);
        }
    }

    // =====================================================================
    //  Acceso a las piezas del servidor (para los manejadores de los equipos)
    // =====================================================================

    /** La lista de conectados, para redirigir mensajes o consultar quien esta en linea. */
    public ClientesConectados getClientes() {
        return clientes;
    }

    /** El log, por si un manejador quiere anotar algo propio. */
    public RegistroLog getLog() {
        return log;
    }

    /** IP y puerto del cliente, por ejemplo "/192.168.1.20:51234". */
    public String getDireccion() {
        return direccion;
    }

    /** Cierra la conexion con este cliente. */
    public void cerrar() {
        cerrado = true;
        try {
            socket.close();
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
    }

    /** Como se identifica este cliente en el log: [ana /192.168.1.20:51234]. */
    private String quien() {
        String nombre = usuario;
        return "[" + (nombre == null ? "sin sesion" : nombre) + " " + direccion + "]";
    }
}
