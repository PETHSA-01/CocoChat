package com.chat.servidor.pruebas;

import com.chat.cliente.ConexionServidor;
import com.chat.common.Peticion;
import com.chat.common.Protocolo;
import com.chat.servidor.ManejadorAuth;
import com.chat.servidor.RegistroLog;
import com.chat.servidor.ServidorSocket;
import com.chat.servidor.bd.ConexionBD;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Prueba del equipo 1 (ManejadorAuth + UsuarioDAO) CON base de datos real.
 *
 * Necesita MySQL encendido en XAMPP. Usa usuarios que empiezan con "prueba_"
 * y los borra al inicio, asi se puede correr las veces que quieran.
 *
 * Ejecutar esta clase: al final imprime TODO BIEN o cuantas pruebas fallaron.
 */
public class PruebaAuth {

    private static final int PUERTO_PRUEBA = 5056;
    private static int fallos = 0;

    private static class ClientePrueba {
        final ConexionServidor conexion = new ConexionServidor();
        final BlockingQueue<String> recibido = new LinkedBlockingQueue<>();

        ClientePrueba() throws Exception {
            conexion.setEscucha(new ConexionServidor.Escucha() {
                @Override
                public void alRecibir(Peticion peticion) {
                    recibido.add(peticion.toString());
                }

                @Override
                public void alDesconectar() {
                }
            });
            conexion.conectar("127.0.0.1", PUERTO_PRUEBA);
        }

        String siguiente() throws InterruptedException {
            return recibido.poll(2, TimeUnit.SECONDS);
        }
    }

    private static void revisar(String nombre, String esperado, String obtenido) {
        boolean bien = esperado.equals(obtenido);
        System.out.println((bien ? "  OK    " : "  FALLA ") + nombre
                + (bien ? "" : "  (esperaba \"" + esperado + "\" y llego \"" + obtenido + "\")"));
        if (!bien) {
            fallos++;
        }
    }

    public static void main(String[] args) throws Exception {
        RegistroLog consola = new RegistroLog() {
            @Override
            public void registrar(String mensaje) {
                System.out.println("    [log] " + mensaje);
            }
        };
        if (!ConexionBD.inicializar(consola)) {
            System.out.println("Enciende MySQL en XAMPP y vuelve a correr la prueba.");
            System.exit(1);
        }
        try (Connection con = ConexionBD.obtener(); Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM usuarios WHERE usuario LIKE 'prueba\\_%'");
        }

        ServidorSocket servidor = new ServidorSocket(PUERTO_PRUEBA, consola);
        servidor.getDespachador().registrar(new ManejadorAuth());
        servidor.iniciar();

        ClientePrueba ana = new ClientePrueba();
        ClientePrueba beto = new ClientePrueba();

        System.out.println("Pruebas:");

        // Registro
        ana.conexion.enviar(Protocolo.REGISTRO, "prueba_ana", "cacahuate", "ana@mail.com",
                "Nombre de mi perro", "Firulais");
        revisar("1. registro de ana", "OK|REGISTRO|prueba_ana", ana.siguiente());
        ana.conexion.enviar(Protocolo.REGISTRO, "prueba_ana", "otra", "x@mail.com", "p", "r");
        revisar("2. usuario repetido", "ERROR|REGISTRO|El usuario ya existe", ana.siguiente());
        ana.conexion.enviar(Protocolo.REGISTRO, "a|b", "cacahuate", "x@mail.com", "p", "r");
        revisar("3. nombre invalido",
                "ERROR|REGISTRO|El usuario debe tener de 3 a 20 letras, numeros, punto o guion bajo",
                ana.siguiente());
        ana.conexion.enviar(Protocolo.REGISTRO, "prueba_x", "cacahuate", "sin-arroba", "p", "r");
        revisar("4. correo invalido", "ERROR|REGISTRO|El correo no es valido", ana.siguiente());
        beto.conexion.enviar(Protocolo.REGISTRO, "prueba_beto", "1234", "beto@mail.com", "Color", "azul");
        revisar("5. registro de beto", "OK|REGISTRO|prueba_beto", beto.siguiente());

        // Login y 3 intentos
        ana.conexion.enviar(Protocolo.LOGIN, "prueba_ana", "mal");
        revisar("6. intento fallido 1", "ERROR|LOGIN|Usuario o contrasena incorrectos|2", ana.siguiente());
        ana.conexion.enviar(Protocolo.LOGIN, "no_existe", "x");
        revisar("7. intento fallido 2", "ERROR|LOGIN|Usuario o contrasena incorrectos|1", ana.siguiente());
        ana.conexion.enviar(Protocolo.LOGIN, "prueba_ana", "mal");
        revisar("8. intento fallido 3 -> 0", "ERROR|LOGIN|Usuario o contrasena incorrectos|0", ana.siguiente());

        // Recuperacion
        ana.conexion.enviar(Protocolo.RECUPERAR, "prueba_ana");
        revisar("9. pide la pregunta", "OK|RECUPERAR|PREGUNTA|Nombre de mi perro", ana.siguiente());
        ana.conexion.enviar(Protocolo.RECUPERAR, "prueba_ana", "michi");
        revisar("10. respuesta mala", "ERROR|RECUPERAR|Respuesta incorrecta", ana.siguiente());
        ana.conexion.enviar(Protocolo.RECUPERAR, "prueba_ana", "  firulais ");
        revisar("11. respuesta buena", "OK|RECUPERAR|CONTRASENA|cacahuate", ana.siguiente());
        ana.conexion.enviar(Protocolo.RECUPERAR, "nadie_aqui");
        revisar("12. recuperar usuario inexistente", "ERROR|RECUPERAR|El usuario no existe", ana.siguiente());

        // El contador volvio a 3
        ana.conexion.enviar(Protocolo.LOGIN, "prueba_ana", "mal");
        revisar("13. contador reiniciado", "ERROR|LOGIN|Usuario o contrasena incorrectos|2", ana.siguiente());

        // Login correcto (con mayusculas distintas) y eventos
        ana.conexion.enviar(Protocolo.LOGIN, "PRUEBA_ANA", "cacahuate");
        revisar("14. login de ana", "OK|LOGIN|prueba_ana", ana.siguiente());
        beto.conexion.enviar(Protocolo.LOGIN, "prueba_beto", "1234");
        revisar("15. login de beto", "OK|LOGIN|prueba_beto", beto.siguiente());
        revisar("16. ana ve a beto conectado", "EVENTO|ESTADO|prueba_beto|1", ana.siguiente());

        ClientePrueba intruso = new ClientePrueba();
        intruso.conexion.enviar(Protocolo.LOGIN, "prueba_ana", "cacahuate");
        revisar("17. sesion duplicada", "ERROR|LOGIN|El usuario ya tiene una sesion abierta", intruso.siguiente());

        // Un registro nuevo aparece en la lista de los conectados
        intruso.conexion.enviar(Protocolo.REGISTRO, "prueba_caro", "abcd", "caro@mail.com", "p", "r");
        revisar("18. registro de caro", "OK|REGISTRO|prueba_caro", intruso.siguiente());
        revisar("19. ana recibe a caro como nuevo", "EVENTO|ESTADO|prueba_caro|0", ana.siguiente());
        revisar("20. beto recibe a caro como nuevo", "EVENTO|ESTADO|prueba_caro|0", beto.siguiente());
        intruso.conexion.cerrar();

        // Lista de usuarios
        ana.conexion.enviar(Protocolo.USUARIOS, "prueba_ana");
        String lista = ana.siguiente();
        revisar("21. lista incluye a beto conectado", "true",
                String.valueOf(lista != null && lista.contains("prueba_beto:1")));
        revisar("22. lista incluye a caro desconectada", "true",
                String.valueOf(lista != null && lista.contains("prueba_caro:0")));
        revisar("23. la lista no incluye a quien pregunta", "false",
                String.valueOf(lista != null && lista.contains("prueba_ana:")));

        // Logout
        beto.conexion.enviar(Protocolo.LOGOUT, "prueba_ana");
        revisar("24. no puede cerrar sesion de otro", "ERROR|LOGOUT|Solo puedes cerrar tu propia sesion",
                beto.siguiente());
        beto.conexion.enviar(Protocolo.LOGOUT, "prueba_beto");
        revisar("25. logout de beto", "OK|LOGOUT", beto.siguiente());
        revisar("26. ana ve a beto desconectado", "EVENTO|ESTADO|prueba_beto|0", ana.siguiente());
        beto.conexion.enviar(Protocolo.USUARIOS, "prueba_beto");
        revisar("27. tras logout ya no hay sesion", "ERROR|USUARIOS|Debes iniciar sesion primero",
                beto.siguiente());

        ana.conexion.cerrar();
        beto.conexion.cerrar();
        servidor.detener();

        System.out.println();
        System.out.println(fallos == 0 ? "TODO BIEN" : "FALLARON " + fallos + " PRUEBA(S)");
        System.exit(fallos == 0 ? 0 : 1);
    }
}
