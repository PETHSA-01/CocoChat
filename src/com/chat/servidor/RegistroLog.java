package com.chat.servidor;

/**
 * Lugar donde el servidor "anota" todo lo que pasa.
 *
 * La ventana de log (VentanaLog) la implementa, pero asi el servidor
 * tambien se puede probar sin ventana (por ejemplo, escribiendo en consola).
 */
public interface RegistroLog {

    /** Escribe una linea en el registro. Se puede llamar desde cualquier hilo. */
    void registrar(String mensaje);
}
