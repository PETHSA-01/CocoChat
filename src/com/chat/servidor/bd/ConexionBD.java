package com.chat.servidor.bd;

import com.chat.servidor.RegistroLog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexion a la base de datos MySQL / MariaDB de XAMPP.
 *
 * La base de datos existe SOLO en la computadora que hace de servidor.
 * Los clientes nunca se conectan a ella: le piden todo al servidor por socket.
 *
 * Antes de arrancar el servidor:
 *   1. Abrir XAMPP y darle Start a MySQL.
 *   2. Listo. Al arrancar, inicializar() crea la base "cocochat" y la tabla
 *      "usuarios" si todavia no existen (tambien se pueden crear a mano con
 *      el script bd/cocochat.sql desde phpMyAdmin).
 *
 * Si tu MySQL tiene otra contrasena para root, cambia CONTRASENA aqui abajo.
 *
 * Cada equipo puede crear sus tablas con su propio script / su propio DAO
 * usando ConexionBD.obtener() para conseguir la conexion.
 */
public final class ConexionBD {

    private ConexionBD() {
        // Clase de utilidades: no se crean objetos de ella.
    }

    // ---------------- Configuracion (la de XAMPP por defecto) ----------------
    public static final String HOST = "localhost";
    public static final int PUERTO = 3306;
    public static final String BASE = "cocochat";
    public static final String USUARIO = "root";
    public static final String CONTRASENA = "";

    private static final String OPCIONES =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

    private static final String URL_SERVIDOR = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + OPCIONES;
    private static final String URL_BASE = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + BASE + OPCIONES;

    /**
     * Abre una conexion nueva a la base "cocochat".
     * Usenla siempre con try-with-resources para que se cierre sola:
     *
     *     try (Connection con = ConexionBD.obtener()) { ... }
     *
     * Se abre una por operacion: asi cada hilo de cliente usa la suya y no
     * se estorban entre ellos.
     */
    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(URL_BASE, USUARIO, CONTRASENA);
    }

    /**
     * Crea la base de datos y las tablas del equipo 1 si no existen.
     * Se llama una vez al arrancar el servidor (ver ServidorSocket.main).
     * Devuelve true si todo quedo listo; si no, lo anota en el log.
     */
    public static boolean inicializar(RegistroLog log) {
        try {
            try (Connection con = DriverManager.getConnection(URL_SERVIDOR, USUARIO, CONTRASENA);
                 Statement st = con.createStatement()) {
                st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + BASE
                        + " CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            }
            try (Connection con = obtener(); Statement st = con.createStatement()) {
                st.executeUpdate(UsuarioDAO.SQL_CREAR_TABLA);
            }
            log.registrar("Base de datos '" + BASE + "' lista en " + HOST + ":" + PUERTO);
            return true;
        } catch (SQLException e) {
            log.registrar("NO SE PUDO CONECTAR A LA BASE DE DATOS (" + e.getMessage()
                    + "). Revisa que MySQL este encendido en XAMPP.");
            return false;
        }
    }
}
