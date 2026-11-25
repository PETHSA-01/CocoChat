package Interfaces;

import java.awt.Color;
import java.awt.Font;
import java.util.ResourceBundle;

public final class Estilos {
    private static final ResourceBundle B = ResourceBundle.getBundle("Estilos");

    private Estilos() {}

    private static String get(String clave, String porDefecto) {
        if (!B.containsKey(clave)) return porDefecto;
        String valor = B.getString(clave);
        // color.fondo=rosa.6  ->  se resuelve a #FBCFFF
        return valor.matches("rosa\\.[1-6]") ? B.getString(valor) : valor;
    }

    public static Color color(String clave) {
        return Color.decode(get(clave, "#FBCFFF"));
    }

    public static int entero(String clave) {
        return Integer.parseInt(get(clave, "0"));
    }

    public static String texto(String clave) {
        return get(clave, "");
    }

    public static Font fuente() {
        return new Font(texto("fuente.familia"), Font.PLAIN, entero("fuente.tamano"));
    }

    public static void aplicar(java.awt.Component c) {
        c.setBackground(color("color.fondo"));
        c.setForeground(color("color.texto"));
        c.setFont(fuente());
    }
}