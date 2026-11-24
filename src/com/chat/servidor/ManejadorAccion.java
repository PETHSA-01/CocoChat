package com.chat.servidor;

import com.chat.common.Peticion;

/**
 * Lo que implementa cada equipo para atender SUS acciones.
 *
 *   ManejadorAuth    -> equipo 1 (REGISTRO, LOGIN, RECUPERAR, LOGOUT, USUARIOS)
 *   ManejadorChats   -> equipo 2 (MENSAJE, AMISTAD_..., HISTORIAL_AMIGO)
 *   ManejadorGrupos  -> equipo 3 (GRUPO_...)
 *
 * Cada equipo escribe su clase en su propio archivo y la registra en el
 * Despachador (ver ServidorSocket.main). Asi nadie toca el archivo de otro.
 *
 * Ejemplo minimo:
 *
 *   public class ManejadorAuth implements ManejadorAccion {
 *       public boolean atiende(String accion) {
 *           return accion.equals(Protocolo.LOGIN) || accion.equals(Protocolo.REGISTRO);
 *       }
 *       public void manejar(Peticion p, ManejadorCliente cliente) {
 *           if (p.getAccion().equals(Protocolo.LOGIN)) {
 *               // ... validar contra la base de datos ...
 *               cliente.iniciarSesion(p.getCampo(0));   // OBLIGATORIO si el login fue correcto
 *               cliente.enviar(Protocolo.ok(Protocolo.LOGIN));
 *           }
 *       }
 *   }
 */
public interface ManejadorAccion {

    /** Dice si este manejador se encarga de esa accion (por ejemplo "LOGIN"). */
    boolean atiende(String accion);

    /**
     * Atiende la peticion. Para responder usen cliente.enviar(...) con
     * Protocolo.ok(...), Protocolo.error(...) o Protocolo.evento(...).
     *
     * Si lanzan IllegalArgumentException (por ejemplo, un campo que falta con
     * p.getCampo(n)), el Despachador responde ERROR al cliente por ustedes.
     */
    void manejar(Peticion peticion, ManejadorCliente cliente);
}
