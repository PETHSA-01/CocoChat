package com.chat.common;

import java.util.Objects;

/**
 * Datos básicos de un usuario que viajan entre cliente y servidor.
 * (La contraseña NO va aquí: solo se usa en REGISTRO y LOGIN.)
 */
public class Usuario {

    private final String nombre;
    private boolean conectado;

    public Usuario(String nombre, boolean conectado) {
        this.nombre = nombre;
        this.conectado = conectado;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isConectado() {
        return conectado;
    }

    public void setConectado(boolean conectado) {
        this.conectado = conectado;
    }

    /** Dos usuarios son el mismo si tienen el mismo nombre. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        return nombre.equals(((Usuario) o).nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    /** Así se ve en un JList de Swing, por ejemplo "ana (conectado)". */
    @Override
    public String toString() {
        return nombre + (conectado ? " (conectado)" : " (desconectado)");
    }
}
