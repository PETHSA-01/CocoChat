package com.chat.servidor.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo lo que se lee o escribe en la tabla "usuarios".
 *
 * Nadie mas escribe SQL de usuarios: si otro equipo necesita saber si un
 * usuario existe, usa new UsuarioDAO().existe(nombre).
 *
 * Todos los metodos usan PreparedStatement (nunca se pegan los datos a mano
 * en el SQL) para que una comilla en un dato no rompa la consulta.
 */
public class UsuarioDAO {

    /** Tabla de usuarios. Se crea sola al arrancar (ConexionBD.inicializar). */
    static final String SQL_CREAR_TABLA =
            "CREATE TABLE IF NOT EXISTS usuarios ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " usuario VARCHAR(20) NOT NULL UNIQUE,"
            + " contrasena VARCHAR(100) NOT NULL,"
            + " correo VARCHAR(100) NOT NULL,"
            + " pregunta VARCHAR(200) NOT NULL,"
            + " respuesta VARCHAR(200) NOT NULL,"
            + " fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    /** Un renglon de la tabla usuarios (solo se usa dentro del servidor). */
    public static class Registro {
        public final String usuario;
        public final String contrasena;
        public final String correo;
        public final String pregunta;
        public final String respuesta;

        Registro(String usuario, String contrasena, String correo, String pregunta, String respuesta) {
            this.usuario = usuario;
            this.contrasena = contrasena;
            this.correo = correo;
            this.pregunta = pregunta;
            this.respuesta = respuesta;
        }
    }

    /**
     * Guarda un usuario nuevo.
     * Devuelve false si el nombre de usuario ya existe (sin importar mayusculas).
     */
    public boolean registrar(String usuario, String contrasena, String correo,
                             String pregunta, String respuesta) throws SQLException {
        String sql = "INSERT INTO usuarios (usuario, contrasena, correo, pregunta, respuesta)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, contrasena);
            ps.setString(3, correo);
            ps.setString(4, pregunta);
            ps.setString(5, respuesta);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false; // usuario repetido (columna UNIQUE)
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                return false; // algunos drivers reportan el duplicado asi
            }
            throw e;
        }
    }

    /** Busca un usuario por nombre. Devuelve null si no existe. */
    public Registro buscar(String usuario) throws SQLException {
        String sql = "SELECT usuario, contrasena, correo, pregunta, respuesta FROM usuarios WHERE usuario = ?";
        try (Connection con = ConexionBD.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Registro(rs.getString("usuario"), rs.getString("contrasena"),
                        rs.getString("correo"), rs.getString("pregunta"), rs.getString("respuesta"));
            }
        }
    }

    /** true si ya hay un usuario con ese nombre. */
    public boolean existe(String usuario) throws SQLException {
        return buscar(usuario) != null;
    }

    /** Nombres de todos los usuarios registrados, en orden alfabetico. */
    public List<String> listarNombres() throws SQLException {
        List<String> nombres = new ArrayList<>();
        String sql = "SELECT usuario FROM usuarios ORDER BY usuario";
        try (Connection con = ConexionBD.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                nombres.add(rs.getString("usuario"));
            }
        }
        return nombres;
    }
}
