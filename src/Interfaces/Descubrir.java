package Interfaces;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Sección «Descubrir» del menú lateral.
 *
 * <p>Existe porque {@link VentanaBase.Seccion#DESCUBRIR} navega a esta ventana;
 * sin la clase el {@code switch} de {@code VentanaBase.ir()} no compilaba.</p>
 *
 * <p>Usa el mismo contenido que Amigos (personas en línea y desconectadas)
 * mientras se conecta la base de datos.</p>
 */
public class Descubrir extends VentanaBase {

    private final List<ItemLista> usuarios = new ArrayList<>();

    public Descubrir() {
        super(Seccion.DESCUBRIR);
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
        // TODO: abrir el perfil del usuario item.nombre()
    }

    /** Punto de entrada para probar solo esta sección (Run File en NetBeans). */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Descubrir().setVisible(true));
    }
}
