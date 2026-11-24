package com.chat.common;

import java.util.ArrayList;
import java.util.List;

/**
 * El "idioma" que hablan el cliente y el servidor.
 *
 * Formato de cada petición (una por línea):
 *     ACCION|campo1|campo2|...
 *
 * Respuestas del servidor:
 *     OK|ACCION|datos...
 *     ERROR|ACCION|motivo
 *
 * Avisos que el servidor manda sin que se los pidan:
 *     EVENTO|TIPO|datos...      (por ejemplo EVENTO|ESTADO|ana|1)
 *
 * REGLA PARA LOS 3 EQUIPOS: nunca escriban "LOGIN|" + usuario + ... a mano.
 * Siempre usen Protocolo.armar(...) para enviar y Protocolo.leer(...) para recibir.
 * Así, si alguien escribe un "|" dentro de su mensaje, no se rompe nada.
 */
public final class Protocolo {

    private Protocolo() {
        // Clase de utilidades: no se crean objetos de ella.
    }

    public static final char SEPARADOR = '|';
    private static final char ESCAPE = '\\';

    // ---------------- Respuestas y avisos ----------------
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String EVENTO = "EVENTO";

    // ---------------- Equipo 1: autenticación ----------------
    /** REGISTRO|usuario|contraseña|correo|pregunta|respuesta */
    public static final String REGISTRO = "REGISTRO";
    /** LOGIN|usuario|contraseña */
    public static final String LOGIN = "LOGIN";
    /** RECUPERAR|usuario|respuesta */
    public static final String RECUPERAR = "RECUPERAR";
    /** LOGOUT|usuario */
    public static final String LOGOUT = "LOGOUT";
    /** USUARIOS|usuario  -> respuesta OK|USUARIOS|ana:1,beto:0 */
    public static final String USUARIOS = "USUARIOS";
    /** Tipo de evento: EVENTO|ESTADO|usuario|1 (conectado) o 0 (desconectado) */
    public static final String ESTADO = "ESTADO";

    // ---------------- Equipo 2: chats y amistades (borrador, que lo ajusten) ----------------
    /** MENSAJE|origen|destino|tipo|texto   (tipo = TEMPORAL o AMIGO) */
    public static final String MENSAJE = "MENSAJE";
    public static final String AMISTAD_SOLICITAR = "AMISTAD_SOLICITAR";
    public static final String AMISTAD_RESPONDER = "AMISTAD_RESPONDER";
    public static final String AMISTAD_ELIMINAR = "AMISTAD_ELIMINAR";
    public static final String HISTORIAL_AMIGO = "HISTORIAL_AMIGO";

    // ---------------- Equipo 3: grupos (borrador, que lo ajusten) ----------------
    public static final String GRUPO_CREAR = "GRUPO_CREAR";
    public static final String GRUPO_RESPONDER = "GRUPO_RESPONDER";
    public static final String GRUPO_MENSAJE = "GRUPO_MENSAJE";
    public static final String GRUPO_AGREGAR = "GRUPO_AGREGAR";
    public static final String GRUPO_SALIR = "GRUPO_SALIR";

    // ---------------- Valores que se repiten en los campos ----------------
    public static final String TEMPORAL = "TEMPORAL";
    public static final String AMIGO = "AMIGO";
    public static final String ACEPTA = "ACEPTA";
    public static final String RECHAZA = "RECHAZA";
    public static final String CONECTADO = "1";
    public static final String DESCONECTADO = "0";

    // =====================================================================
    //  ARMAR: de datos a cadena
    // =====================================================================

    /**
     * Arma la cadena que se envía por el socket.
     * Ejemplo: armar("LOGIN", "ana", "cacahuate")  ->  "LOGIN|ana|cacahuate"
     */
    public static String armar(String accion, String... campos) {
        if (accion == null || accion.isEmpty()) {
            throw new IllegalArgumentException("La acción no puede estar vacía");
        }
        StringBuilder sb = new StringBuilder(accion);
        for (String campo : campos) {
            sb.append(SEPARADOR).append(escapar(campo == null ? "" : campo));
        }
        return sb.toString();
    }

    /** Atajo para el servidor: ok("LOGIN") -> "OK|LOGIN" */
    public static String ok(String accion, String... datos) {
        return armar(OK, unir(accion, datos));
    }

    /** Atajo para el servidor: error("LOGIN", "contraseña incorrecta") -> "ERROR|LOGIN|contraseña incorrecta" */
    public static String error(String accion, String motivo) {
        return armar(ERROR, accion, motivo);
    }

    /** Atajo para el servidor: evento("ESTADO", "ana", "1") -> "EVENTO|ESTADO|ana|1" */
    public static String evento(String tipo, String... datos) {
        return armar(EVENTO, unir(tipo, datos));
    }

    // =====================================================================
    //  LEER: de cadena a datos
    // =====================================================================

    /**
     * Lee una línea recibida y la separa en acción y campos.
     * Ejemplo: leer("LOGIN|ana|cacahuate") -> accion "LOGIN", campos ["ana", "cacahuate"]
     */
    public static Peticion leer(String linea) {
        if (linea == null || linea.isEmpty()) {
            throw new IllegalArgumentException("Se recibió una línea vacía");
        }
        List<String> partes = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == ESCAPE && i + 1 < linea.length()) {
                // Carácter escapado: se toma literal el siguiente.
                char siguiente = linea.charAt(++i);
                if (siguiente == 'n') {
                    actual.append('\n');
                } else {
                    actual.append(siguiente); // \| -> |   y   \\ -> \
                }
            } else if (c == SEPARADOR) {
                partes.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        partes.add(actual.toString());

        String accion = partes.remove(0);
        return new Peticion(accion, partes);
    }

    // =====================================================================
    //  ESCAPAR: para que un "|" o un salto de línea dentro de un texto no rompa la cadena
    // =====================================================================

    /** "hola|adiós" -> "hola\|adiós"   y   salto de línea -> "\n" */
    public static String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == ESCAPE) {
                sb.append("\\\\");
            } else if (c == SEPARADOR) {
                sb.append("\\|");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c != '\r') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // =====================================================================
    //  LISTA DE USUARIOS: "ana:1,beto:0"
    // =====================================================================

    /** Convierte la lista en "ana:1,beto:0" para la respuesta de USUARIOS. */
    public static String armarListaUsuarios(List<Usuario> usuarios) {
        StringBuilder sb = new StringBuilder();
        for (Usuario u : usuarios) {
            if (sb.length() > 0) sb.append(',');
            sb.append(u.getNombre()).append(':').append(u.isConectado() ? CONECTADO : DESCONECTADO);
        }
        return sb.toString();
    }

    /** Convierte "ana:1,beto:0" de vuelta en una lista de Usuario. */
    public static List<Usuario> leerListaUsuarios(String texto) {
        List<Usuario> lista = new ArrayList<>();
        if (texto == null || texto.isEmpty()) return lista;
        for (String par : texto.split(",")) {
            String[] datos = par.split(":");
            if (datos.length == 2) {
                lista.add(new Usuario(datos[0], CONECTADO.equals(datos[1])));
            }
        }
        return lista;
    }

    /**
     * Regla para nombres de usuario: 3 a 20 letras, números, punto o guion bajo.
     * Así nunca traen "|", ":" o "," que romperían la lista de usuarios.
     * El registro (cliente y servidor) debe usar esta validación.
     */
    public static boolean esNombreValido(String nombre) {
        return nombre != null && nombre.matches("[A-Za-z0-9._]{3,20}");
    }

    // ---------------- auxiliar interno ----------------
    private static String[] unir(String primero, String[] resto) {
        String[] todo = new String[resto.length + 1];
        todo[0] = primero;
        System.arraycopy(resto, 0, todo, 1, resto.length);
        return todo;
    }
}
