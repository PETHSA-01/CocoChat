package com.chat.servidor;

import com.chat.common.Peticion;
import com.chat.common.Protocolo;
import com.chat.common.Usuario;
import com.chat.servidor.bd.UsuarioDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * EQUIPO 1 - Autenticacion: registro, login con 3 intentos, recuperacion,
 * logout y lista de usuarios con su estado.
 *
 * ===================== Cadenas que atiende =====================
 *
 * REGISTRO|usuario|contrasena|correo|pregunta|respuesta
 *     -> OK|REGISTRO|usuario
 *     -> ERROR|REGISTRO|motivo                     (usuario ya existe, datos invalidos...)
 *     Ademas avisa a los conectados: EVENTO|ESTADO|usuario|0
 *     (asi el nuevo aparece en su lista como desconectado).
 *
 * LOGIN|usuario|contrasena
 *     -> OK|LOGIN|usuario
 *     -> ERROR|LOGIN|Usuario o contrasena incorrectos|N   (N = intentos restantes: 2, 1 o 0)
 *     -> ERROR|LOGIN|El usuario ya tiene una sesion abierta   (no cuenta como intento)
 *     Cuando N llega a 0 el cliente manda al usuario a RECUPERAR y el contador
 *     vuelve a empezar en 3. Los demas reciben EVENTO|ESTADO|usuario|1.
 *
 * RECUPERAR|usuario                      (paso 1: pedir la pregunta de seguridad)
 *     -> OK|RECUPERAR|PREGUNTA|texto de la pregunta
 *     -> ERROR|RECUPERAR|El usuario no existe
 * RECUPERAR|usuario|respuesta            (paso 2: contestarla)
 *     -> OK|RECUPERAR|CONTRASENA|la contrasena guardada
 *     -> ERROR|RECUPERAR|Respuesta incorrecta
 *     (la respuesta no distingue mayusculas ni espacios al inicio / final)
 *
 * LOGOUT|usuario
 *     -> OK|LOGOUT     y los demas reciben EVENTO|ESTADO|usuario|0
 *
 * USUARIOS|usuario
 *     -> OK|USUARIOS|ana:1,beto:0,...   (todos los registrados menos quien pregunta;
 *                                        1 = conectado, 0 = desconectado)
 *
 * Los EVENTO|ESTADO de login / logout / desconexion los manda ManejadorCliente
 * (iniciarSesion y cerrarSesion); aqui solo se llaman.
 */
public class ManejadorAuth implements ManejadorAccion {

    public static final int INTENTOS_MAXIMOS = 3;

    /** Tipos de respuesta de RECUPERAR (primer dato despues de OK|RECUPERAR). */
    public static final String PREGUNTA = "PREGUNTA";
    public static final String CONTRASENA = "CONTRASENA";

    private static final String ERROR_BD = "No se pudo acceder a la base de datos del servidor";

    private final UsuarioDAO dao = new UsuarioDAO();

    /**
     * Intentos fallidos de login por conexion. Es WeakHashMap para que, si el
     * cliente se va, su contador desaparezca solo.
     */
    private final Map<ManejadorCliente, Integer> fallos =
            Collections.synchronizedMap(new WeakHashMap<ManejadorCliente, Integer>());

    @Override
    public boolean atiende(String accion) {
        return accion.equals(Protocolo.REGISTRO)
                || accion.equals(Protocolo.LOGIN)
                || accion.equals(Protocolo.RECUPERAR)
                || accion.equals(Protocolo.LOGOUT)
                || accion.equals(Protocolo.USUARIOS);
    }

    @Override
    public void manejar(Peticion p, ManejadorCliente cliente) {
        try {
            switch (p.getAccion()) {
                case Protocolo.REGISTRO:
                    registro(p, cliente);
                    break;
                case Protocolo.LOGIN:
                    login(p, cliente);
                    break;
                case Protocolo.RECUPERAR:
                    recuperar(p, cliente);
                    break;
                case Protocolo.LOGOUT:
                    logout(p, cliente);
                    break;
                case Protocolo.USUARIOS:
                    usuarios(p, cliente);
                    break;
                default:
                    break;
            }
        } catch (SQLException e) {
            cliente.getLog().registrar("Error de base de datos en " + p.getAccion() + ": " + e.getMessage());
            cliente.enviar(Protocolo.error(p.getAccion(), ERROR_BD));
        }
    }

    // =====================================================================
    //  REGISTRO|usuario|contrasena|correo|pregunta|respuesta
    // =====================================================================

    private void registro(Peticion p, ManejadorCliente cliente) throws SQLException {
        if (!p.tieneCampos(5)) {
            cliente.enviar(Protocolo.error(Protocolo.REGISTRO, "Faltan datos del registro"));
            return;
        }
        String usuario = p.getCampo(0).trim();
        String contrasena = p.getCampo(1);
        String correo = p.getCampo(2).trim();
        String pregunta = p.getCampo(3).trim();
        String respuesta = p.getCampo(4).trim();

        String problema = validarRegistro(usuario, contrasena, correo, pregunta, respuesta);
        if (problema != null) {
            cliente.enviar(Protocolo.error(Protocolo.REGISTRO, problema));
            return;
        }

        if (!dao.registrar(usuario, contrasena, correo, pregunta, respuesta)) {
            cliente.enviar(Protocolo.error(Protocolo.REGISTRO, "El usuario ya existe"));
            return;
        }

        cliente.getLog().registrar("Se registro el usuario " + usuario + " en la base de datos");
        cliente.enviar(Protocolo.ok(Protocolo.REGISTRO, usuario));
        // Los que ya estan dentro lo agregan a su lista como desconectado.
        cliente.getClientes().difundir(
                Protocolo.evento(Protocolo.ESTADO, usuario, Protocolo.DESCONECTADO), null);
    }

    /** Devuelve el motivo del error, o null si todo esta bien. */
    private String validarRegistro(String usuario, String contrasena, String correo,
                                   String pregunta, String respuesta) {
        if (!Protocolo.esNombreValido(usuario)) {
            return "El usuario debe tener de 3 a 20 letras, numeros, punto o guion bajo";
        }
        if (contrasena.length() < 4 || contrasena.length() > 50) {
            return "La contrasena debe tener de 4 a 50 caracteres";
        }
        if (correo.length() > 100 || !correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return "El correo no es valido";
        }
        if (pregunta.isEmpty() || pregunta.length() > 200) {
            return "Escribe una pregunta de seguridad (maximo 200 caracteres)";
        }
        if (respuesta.isEmpty() || respuesta.length() > 200) {
            return "Escribe la respuesta de seguridad (maximo 200 caracteres)";
        }
        return null;
    }

    // =====================================================================
    //  LOGIN|usuario|contrasena   (regla de los 3 intentos)
    // =====================================================================

    private void login(Peticion p, ManejadorCliente cliente) throws SQLException {
        if (!p.tieneCampos(2)) {
            cliente.enviar(Protocolo.error(Protocolo.LOGIN, "Faltan el usuario o la contrasena"));
            return;
        }
        String usuario = p.getCampo(0).trim();
        String contrasena = p.getCampo(1);

        UsuarioDAO.Registro registro = usuario.isEmpty() ? null : dao.buscar(usuario);
        if (registro == null || !registro.contrasena.equals(contrasena)) {
            int restantes = sumarFallo(cliente);
            cliente.getLog().registrar("Login fallido de " + usuario + ". Intentos restantes: " + restantes);
            cliente.enviar(Protocolo.armar(Protocolo.ERROR, Protocolo.LOGIN,
                    "Usuario o contrasena incorrectos", String.valueOf(restantes)));
            return;
        }

        // Se usa el nombre tal como esta guardado (por si escribio "ANA" en vez de "ana").
        if (!cliente.iniciarSesion(registro.usuario)) {
            cliente.enviar(Protocolo.error(Protocolo.LOGIN, "El usuario ya tiene una sesion abierta"));
            return;
        }
        fallos.remove(cliente);
        cliente.enviar(Protocolo.ok(Protocolo.LOGIN, registro.usuario));
    }

    /** Suma un intento fallido y devuelve cuantos quedan (al llegar a 0 se reinicia). */
    private int sumarFallo(ManejadorCliente cliente) {
        synchronized (fallos) {
            Integer anteriores = fallos.get(cliente);
            int total = (anteriores == null ? 0 : anteriores) + 1;
            int restantes = INTENTOS_MAXIMOS - total;
            if (restantes <= 0) {
                fallos.remove(cliente); // el cliente lo manda a recuperar; luego vuelve a tener 3
                return 0;
            }
            fallos.put(cliente, total);
            return restantes;
        }
    }

    // =====================================================================
    //  RECUPERAR|usuario   y   RECUPERAR|usuario|respuesta
    // =====================================================================

    private void recuperar(Peticion p, ManejadorCliente cliente) throws SQLException {
        if (p.cantidadCampos() != 1 && p.cantidadCampos() != 2) {
            cliente.enviar(Protocolo.error(Protocolo.RECUPERAR, "Faltan datos para recuperar"));
            return;
        }
        UsuarioDAO.Registro registro = dao.buscar(p.getCampo(0).trim());
        if (registro == null) {
            cliente.enviar(Protocolo.error(Protocolo.RECUPERAR, "El usuario no existe"));
            return;
        }

        // Paso 1: solo manda la pregunta para que el cliente la muestre.
        if (p.cantidadCampos() == 1) {
            cliente.enviar(Protocolo.ok(Protocolo.RECUPERAR, PREGUNTA, registro.pregunta));
            return;
        }

        // Paso 2: revisar la respuesta.
        String respuesta = p.getCampo(1).trim();
        if (!respuesta.equalsIgnoreCase(registro.respuesta.trim())) {
            cliente.enviar(Protocolo.error(Protocolo.RECUPERAR, "Respuesta incorrecta"));
            return;
        }
        fallos.remove(cliente);
        cliente.getLog().registrar("Se recuperaron las credenciales de " + registro.usuario);
        cliente.enviar(Protocolo.ok(Protocolo.RECUPERAR, CONTRASENA, registro.contrasena));
    }

    // =====================================================================
    //  LOGOUT|usuario
    // =====================================================================

    private void logout(Peticion p, ManejadorCliente cliente) {
        if (!p.tieneCampos(1) || !p.getCampo(0).equals(cliente.getUsuario())) {
            cliente.enviar(Protocolo.error(Protocolo.LOGOUT, "Solo puedes cerrar tu propia sesion"));
            return;
        }
        cliente.enviar(Protocolo.ok(Protocolo.LOGOUT));
        cliente.cerrarSesion(); // avisa EVENTO|ESTADO|usuario|0 a los demas
    }

    // =====================================================================
    //  USUARIOS|usuario  ->  OK|USUARIOS|ana:1,beto:0
    // =====================================================================

    private void usuarios(Peticion p, ManejadorCliente cliente) throws SQLException {
        String yo = cliente.getUsuario();
        if (!p.tieneCampos(1) || !p.getCampo(0).equals(yo)) {
            cliente.enviar(Protocolo.error(Protocolo.USUARIOS, "Peticion de otro usuario"));
            return;
        }
        ClientesConectados conectados = cliente.getClientes();
        List<Usuario> lista = new ArrayList<>();
        for (String nombre : dao.listarNombres()) {
            if (!nombre.equals(yo)) {
                lista.add(new Usuario(nombre, conectados.estaConectado(nombre)));
            }
        }
        cliente.enviar(Protocolo.ok(Protocolo.USUARIOS, Protocolo.armarListaUsuarios(lista)));
    }
}
