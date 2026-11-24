package com.chat.servidor;

import com.chat.common.Peticion;
import com.chat.common.Protocolo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * El "recepcionista": segun la ACCION de la peticion, se la pasa al manejador
 * del equipo que la atiende.
 *
 * Cada equipo registra su manejador una sola vez (ver ServidorSocket.main):
 *
 *     despachador.registrar(new ManejadorAuth());     // equipo 1
 *     despachador.registrar(new ManejadorChats());    // equipo 2
 *     despachador.registrar(new ManejadorGrupos());   // equipo 3
 *
 * Reglas que aplica antes de entregar la peticion:
 *   - Solo REGISTRO, LOGIN y RECUPERAR se aceptan sin haber iniciado sesion.
 *     Todo lo demas responde ERROR|ACCION|Debes iniciar sesion primero.
 *   - Si nadie atiende la accion, responde ERROR|ACCION|Accion desconocida...
 *   - Si un manejador lanza una excepcion, responde ERROR en lugar de tirar el hilo.
 */
public class Despachador {

    /** Acciones que se pueden pedir sin haber iniciado sesion. */
    private static final Set<String> ACCIONES_SIN_SESION = new HashSet<>(Arrays.asList(
            Protocolo.REGISTRO, Protocolo.LOGIN, Protocolo.RECUPERAR));

    private final List<ManejadorAccion> manejadores = new CopyOnWriteArrayList<>();
    private final RegistroLog log;
    private volatile boolean exigirSesion = true;

    public Despachador(RegistroLog log) {
        this.log = log;
    }

    /** Agrega el manejador de un equipo. */
    public void registrar(ManejadorAccion manejador) {
        manejadores.add(manejador);
    }

    /**
     * Si es false, deja pasar cualquier accion aunque no haya sesion
     * (solo para probar mientras el login todavia no esta listo).
     */
    public void setExigirSesion(boolean exigirSesion) {
        this.exigirSesion = exigirSesion;
    }

    // =====================================================================
    //  Entregar la peticion al manejador correcto
    // =====================================================================

    public void despachar(Peticion peticion, ManejadorCliente cliente) {
        String accion = peticion.getAccion();

        if (exigirSesion && !ACCIONES_SIN_SESION.contains(accion) && !cliente.estaAutenticado()) {
            cliente.enviar(Protocolo.error(accion, "Debes iniciar sesion primero"));
            return;
        }

        for (ManejadorAccion manejador : manejadores) {
            if (manejador.atiende(accion)) {
                try {
                    manejador.manejar(peticion, cliente);
                } catch (IllegalArgumentException e) {
                    cliente.enviar(Protocolo.error(accion, "Peticion mal formada: " + e.getMessage()));
                } catch (RuntimeException e) {
                    log.registrar("Error al atender " + accion + ": " + e);
                    cliente.enviar(Protocolo.error(accion, "Error interno del servidor"));
                }
                return;
            }
        }

        cliente.enviar(Protocolo.error(accion, "Accion desconocida o todavia no disponible"));
    }

    // =====================================================================
    //  Describir la peticion para la ventana de log
    // =====================================================================

    /**
     * Convierte la peticion en una frase para el log. Muestra TODO en texto
     * visible, contrasenas incluidas (solo es para que el profesor revise).
     * Ejemplo: "Recibi una peticion para enviar un mensaje de ana a beto. El mensaje dice: hola"
     */
    public String describir(Peticion p) {
        switch (p.getAccion()) {
            case Protocolo.REGISTRO:
                if (p.tieneCampos(5)) {
                    return "Recibi una peticion de registro. Usuario: " + p.getCampo(0)
                            + ". Contrasena: " + p.getCampo(1)
                            + ". Correo: " + p.getCampo(2)
                            + ". Pregunta de seguridad: " + p.getCampo(3)
                            + ". Respuesta: " + p.getCampo(4);
                }
                break;
            case Protocolo.LOGIN:
                if (p.tieneCampos(2)) {
                    return "Recibi una peticion de inicio de sesion de " + p.getCampo(0)
                            + ". La contrasena es: " + p.getCampo(1);
                }
                break;
            case Protocolo.RECUPERAR:
                if (p.tieneCampos(2)) {
                    return "Recibi una peticion para recuperar las credenciales de " + p.getCampo(0)
                            + ". La respuesta de seguridad es: " + p.getCampo(1);
                }
                break;
            case Protocolo.LOGOUT:
                if (p.tieneCampos(1)) {
                    return "Recibi una peticion de cierre de sesion de " + p.getCampo(0);
                }
                break;
            case Protocolo.USUARIOS:
                if (p.tieneCampos(1)) {
                    return "Recibi una peticion de la lista de usuarios de " + p.getCampo(0);
                }
                break;
            case Protocolo.MENSAJE:
                if (p.tieneCampos(4)) {
                    return "Recibi una peticion para enviar un mensaje de " + p.getCampo(0)
                            + " a " + p.getCampo(1) + ". El mensaje dice: " + p.getCampo(3)
                            + " (tipo " + p.getCampo(2) + ")";
                }
                break;
            default:
                break;
        }
        return "Recibi la peticion: " + p;
    }
}
