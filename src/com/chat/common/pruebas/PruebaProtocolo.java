package com.chat.common.pruebas;

import com.chat.common.Peticion;
import com.chat.common.Protocolo;
import com.chat.common.Usuario;

import java.util.Arrays;
import java.util.List;

/**
 * Prueba rápida del protocolo. En NetBeans: clic derecho en este archivo -> Run File (Shift+F6).
 * Cada línea debe decir "BIEN". Si alguna dice "FALLA", algo se rompió.
 */
public class PruebaProtocolo {

    private static int fallas = 0;

    public static void main(String[] args) {
        // 1. Armar una petición sencilla
        revisar("armar LOGIN",
                Protocolo.armar(Protocolo.LOGIN, "ana", "cacahuate"),
                "LOGIN|ana|cacahuate");

        // 2. Leerla de regreso
        Peticion p = Protocolo.leer("LOGIN|ana|cacahuate");
        revisar("leer acción", p.getAccion(), "LOGIN");
        revisar("leer campo 0", p.getCampo(0), "ana");
        revisar("leer campo 1", p.getCampo(1), "cacahuate");

        // 3. Un mensaje con "|" y salto de línea adentro no debe romperse
        String texto = "hola | ¿cómo vas?\nsegunda línea";
        String cadena = Protocolo.armar(Protocolo.MENSAJE, "ana", "beto", Protocolo.TEMPORAL, texto);
        Peticion m = Protocolo.leer(cadena);
        revisar("mensaje con | trae 4 campos", String.valueOf(m.cantidadCampos()), "4");
        revisar("texto del mensaje intacto", m.getCampo(3), texto);

        // 4. Campos vacíos se conservan
        Peticion v = Protocolo.leer(Protocolo.armar("REGISTRO", "ana", "", "correo@x.com"));
        revisar("campo vacío", v.getCampo(1), "");

        // 5. Respuestas y eventos
        revisar("ok", Protocolo.ok(Protocolo.LOGIN), "OK|LOGIN");
        revisar("error", Protocolo.error(Protocolo.LOGIN, "contraseña incorrecta"),
                "ERROR|LOGIN|contraseña incorrecta");
        revisar("evento", Protocolo.evento(Protocolo.ESTADO, "ana", Protocolo.CONECTADO),
                "EVENTO|ESTADO|ana|1");

        // 6. Lista de usuarios
        List<Usuario> lista = Arrays.asList(new Usuario("ana", true), new Usuario("beto", false));
        String textoLista = Protocolo.armarListaUsuarios(lista);
        revisar("armar lista", textoLista, "ana:1,beto:0");
        List<Usuario> leida = Protocolo.leerListaUsuarios(textoLista);
        revisar("leer lista", leida.get(1).getNombre() + "/" + leida.get(1).isConectado(), "beto/false");

        // 7. Validación de nombres
        revisar("nombre válido", String.valueOf(Protocolo.esNombreValido("ana_23")), "true");
        revisar("nombre con | no válido", String.valueOf(Protocolo.esNombreValido("an|a")), "false");

        System.out.println();
        System.out.println(fallas == 0 ? "Todo bien." : fallas + " prueba(s) fallaron.");
    }

    private static void revisar(String nombre, String obtenido, String esperado) {
        boolean bien = esperado.equals(obtenido);
        if (!bien) fallas++;
        System.out.println((bien ? "BIEN   " : "FALLA  ") + nombre
                + (bien ? "" : "  -> esperaba [" + esperado + "] y obtuve [" + obtenido + "]"));
    }
}
