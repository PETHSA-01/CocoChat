package com.chat.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Una petición (o respuesta) ya leída: la ACCION más sus campos.
 *
 * Ejemplo: la cadena "LOGIN|ana|cacahuate" se convierte en
 *   accion = "LOGIN"
 *   campos = ["ana", "cacahuate"]
 *
 * No se crea a mano al recibir: se obtiene con Protocolo.leer(linea).
 */
public class Peticion {

    private final String accion;
    private final List<String> campos;

    public Peticion(String accion, List<String> campos) {
        if (accion == null || accion.isEmpty()) {
            throw new IllegalArgumentException("La acción no puede estar vacía");
        }
        this.accion = accion;
        this.campos = Collections.unmodifiableList(new ArrayList<>(campos));
    }

    /** La acción, por ejemplo "LOGIN", "MENSAJE", "OK", "ERROR" o "EVENTO". */
    public String getAccion() {
        return accion;
    }

    /** Todos los campos después de la acción (no se pueden modificar). */
    public List<String> getCampos() {
        return campos;
    }

    /** Cuántos campos trae (sin contar la acción). */
    public int cantidadCampos() {
        return campos.size();
    }

    /**
     * Devuelve el campo en la posición indicada (empieza en 0).
     * Si no existe, lanza un error claro en lugar de un IndexOutOfBounds.
     */
    public String getCampo(int posicion) {
        if (posicion < 0 || posicion >= campos.size()) {
            throw new IllegalArgumentException("La petición " + accion + " no tiene el campo "
                    + posicion + " (trae " + campos.size() + " campos)");
        }
        return campos.get(posicion);
    }

    /** Sirve para validar en el servidor antes de usar los campos. */
    public boolean tieneCampos(int cantidadEsperada) {
        return campos.size() == cantidadEsperada;
    }

    /** Vuelve a convertir la petición en cadena (útil para la ventana de log). */
    @Override
    public String toString() {
        return Protocolo.armar(accion, campos.toArray(new String[0]));
    }
}
