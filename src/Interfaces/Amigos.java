package Interfaces;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

public class Amigos extends VentanaBase {

    private final List<ItemLista> usuarios = new ArrayList<>();

    public Amigos() {
        super(Seccion.AMIGOS);
        // Card de prueba: bórrala cuando conectes la base de datos
        usuarios.add(new ItemLista("Meow", true));
        iniciar();
    }

    /** Reemplaza las cards por las que vengan de la base de datos. */
    public void setUsuarios(List<ItemLista> lista) {
        usuarios.clear();
        usuarios.addAll(lista);
        reconstruir();
    }

    @Override protected JComponent crearContenido() {
        return contenidoUsuarios(usuarios, "No hay usuarios");
    }

    @Override protected void alSeleccionar(ItemLista item) {
        // TODO: abrir el chat con item.nombre()
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new Amigos().setVisible(true));
    }
}