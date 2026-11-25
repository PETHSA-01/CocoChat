package Interfaces;

import static Interfaces.Inicio.color;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

public class Grupo extends VentanaBase {

    private final List<ItemLista> grupos = new ArrayList<>();

    public Grupo() {
        super(Seccion.GRUPOS);
        // Card de prueba: bórrala cuando conectes la base de datos
        grupos.add(new ItemLista("Meow", false));
        iniciar();
    }

    /** Reemplaza las cards por las que vengan de la base de datos. */
    public void setGrupos(List<ItemLista> lista) {
        grupos.clear();
        grupos.addAll(lista);
        reconstruir();
    }

    @Override protected JComponent crearContenido() {
        if (grupos.isEmpty()) return mensajeVacio("No hay grupos");
        return apilar(List.of(
            seccion("Grupos", color("seccion.activa"), grupos, TipoAvatar.GRUPO, false)));
    }

    @Override protected void alSeleccionar(ItemLista item) {
        // TODO: abrir el chat del grupo item.nombre()
    }
}