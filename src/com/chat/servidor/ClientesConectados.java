package com.chat.servidor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La lista de clientes conectados: usuario -> su conexion.
 *
 * Sirve para dos cosas:
 *   1. Redirigir: enviarA("beto", cadena) manda algo a Beto sin que el emisor
 *      sepa como hablarle (los clientes nunca se hablan directamente).
 *   2. Avisar: difundir(cadena, "ana") manda un aviso a todos menos a Ana.
 *
 * Solo aparecen aqui los clientes que YA iniciaron sesion. Es segura para
 * usarse desde varios hilos al mismo tiempo.
 */
public class ClientesConectados {

    private final Map<String, ManejadorCliente> porUsuario = new ConcurrentHashMap<>();

    /**
     * Anota que el usuario ya esta conectado con esa conexion.
     * Devuelve false si ese usuario ya estaba conectado desde otra conexion.
     */
    public boolean registrar(String usuario, ManejadorCliente cliente) {
        ManejadorCliente anterior = porUsuario.putIfAbsent(usuario, cliente);
        return anterior == null || anterior == cliente;
    }

    /** Lo quita de la lista (solo si sigue siendo esa misma conexion). */
    public void quitar(String usuario, ManejadorCliente cliente) {
        porUsuario.remove(usuario, cliente);
    }

    public boolean estaConectado(String usuario) {
        return porUsuario.containsKey(usuario);
    }

    /** La conexion de ese usuario, o null si no esta conectado. */
    public ManejadorCliente obtener(String usuario) {
        return porUsuario.get(usuario);
    }

    /** Nombres de todos los usuarios conectados en este momento. */
    public List<String> nombres() {
        return new ArrayList<>(porUsuario.keySet());
    }

    public int cantidad() {
        return porUsuario.size();
    }

    /**
     * Manda una cadena ya armada a un usuario.
     * Devuelve false si ese usuario no esta conectado.
     */
    public boolean enviarA(String usuario, String cadena) {
        ManejadorCliente cliente = porUsuario.get(usuario);
        if (cliente == null) {
            return false;
        }
        cliente.enviar(cadena);
        return true;
    }

    /** Manda una cadena a todos los conectados, menos a "excepto" (puede ser null). */
    public void difundir(String cadena, String excepto) {
        for (Map.Entry<String, ManejadorCliente> entrada : porUsuario.entrySet()) {
            if (!entrada.getKey().equals(excepto)) {
                entrada.getValue().enviar(cadena);
            }
        }
    }
}
