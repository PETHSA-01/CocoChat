package com.chat.servidor.pruebas;

import com.chat.cliente.ConexionServidor;
import com.chat.common.Peticion;
import com.chat.common.Protocolo;
import com.chat.servidor.ManejadorAccion;
import com.chat.servidor.ManejadorCliente;
import com.chat.servidor.RegistroLog;
import com.chat.servidor.ServidorSocket;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Prueba del servidor SIN ventana y SIN base de datos.
 *
 * Levanta el servidor en un puerto de prueba, le conecta dos clientes con la
 * clase ConexionServidor y revisa: sesion obligatoria, avisos de conectado /
 * desconectado, redireccion de mensajes y el texto del log.
 *
 * Usa un manejador de juguete (LOGIN, LOGOUT y MENSAJE) que solo existe aqui;
 * los reales son ManejadorAuth, ManejadorChats y ManejadorGrupos.
 *
 * Ejecutar esta clase: al final imprime TODO BIEN o cuantas pruebas fallaron.
 */
public class PruebaServidor {

    private static final int PUERTO_PRUEBA = 5055;
    private static int fallos = 0;

    /** Un cliente de prueba: guarda en una cola todo lo que llega del servidor. */
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

        /** Espera hasta 2 segundos a que llegue algo; devuelve null si no llego nada. */
        String siguiente() throws InterruptedException {
            return recibido.poll(2, TimeUnit.SECONDS);
        }
    }

    /** Manejador de juguete: hace el minimo para probar el servidor. */
    private static class ManejadorDeJuguete implements ManejadorAccion {
        @Override
        public boolean atiende(String accion) {
            return accion.equals(Protocolo.LOGIN) || accion.equals(Protocolo.LOGOUT)
                    || accion.equals(Protocolo.MENSAJE);
        }

        @Override
        public void manejar(Peticion p, ManejadorCliente cliente) {
            switch (p.getAccion()) {
                case Protocolo.LOGIN:
                    if (cliente.iniciarSesion(p.getCampo(0))) {
                        cliente.enviar(Protocolo.ok(Protocolo.LOGIN));
                    } else {
                        cliente.enviar(Protocolo.error(Protocolo.LOGIN, "usuario ya conectado"));
                    }
                    break;
                case Protocolo.LOGOUT:
                    cliente.cerrarSesion();
                    cliente.enviar(Protocolo.ok(Protocolo.LOGOUT));
                    break;
                case Protocolo.MENSAJE:
                    // MENSAJE|origen|destino|tipo|texto  -> se redirige al destino
                    boolean entregado = cliente.getClientes().enviarA(p.getCampo(1), p.toString());
                    cliente.enviar(entregado
                            ? Protocolo.ok(Protocolo.MENSAJE)
                            : Protocolo.error(Protocolo.MENSAJE, "destino no conectado"));
                    break;
                default:
                    break;
            }
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
        final List<String> lineasLog = new CopyOnWriteArrayList<>();
        ServidorSocket servidor = new ServidorSocket(PUERTO_PRUEBA, new RegistroLog() {
            @Override
            public void registrar(String mensaje) {
                lineasLog.add(mensaje);
                System.out.println("    [log] " + mensaje);
            }
        });
        servidor.getDespachador().registrar(new ManejadorDeJuguete());
        servidor.iniciar();

        ClientePrueba ana = new ClientePrueba();
        ClientePrueba beto = new ClientePrueba();

        System.out.println("Pruebas:");

        // 1. Sin sesion no se puede hacer nada mas que REGISTRO / LOGIN / RECUPERAR
        ana.conexion.enviar(Protocolo.MENSAJE, "ana", "beto", Protocolo.TEMPORAL, "hola");
        revisar("1. sin sesion se rechaza", "ERROR|MENSAJE|Debes iniciar sesion primero", ana.siguiente());

        // 2. Login de ana y de beto; ana se entera de que beto entro
        ana.conexion.enviar(Protocolo.LOGIN, "ana", "cacahuate");
        revisar("2. login de ana", "OK|LOGIN", ana.siguiente());
        beto.conexion.enviar(Protocolo.LOGIN, "beto", "1234");
        revisar("3. login de beto", "OK|LOGIN", beto.siguiente());
        revisar("4. ana recibe EVENTO de beto conectado", "EVENTO|ESTADO|beto|1", ana.siguiente());

        // 3. Una tercera conexion no puede entrar con un usuario ya conectado
        ClientePrueba intruso = new ClientePrueba();
        intruso.conexion.enviar(Protocolo.LOGIN, "ana", "otra");
        revisar("5. mismo usuario dos veces se rechaza", "ERROR|LOGIN|usuario ya conectado", intruso.siguiente());
        intruso.conexion.cerrar();

        // 4. Redireccion: ana le escribe a beto y el servidor se lo entrega
        ana.conexion.enviar(Protocolo.MENSAJE, "ana", "beto", Protocolo.TEMPORAL, "hola | con barra");
        revisar("6. beto recibe el mensaje de ana",
                "MENSAJE|ana|beto|TEMPORAL|hola \\| con barra", beto.siguiente());
        revisar("7. ana recibe confirmacion", "OK|MENSAJE", ana.siguiente());

        // 5. Accion que nadie atiende
        ana.conexion.enviar("INVENTADA", "x");
        revisar("8. accion desconocida", "ERROR|INVENTADA|Accion desconocida o todavia no disponible", ana.siguiente());

        // 6. Linea que no es una peticion valida
        ana.conexion.enviar("|vacia");
        revisar("9. linea invalida", "ERROR|DESCONOCIDO|Linea invalida", ana.siguiente());

        // 7. beto se desconecta de golpe: ana debe enterarse
        beto.conexion.cerrar();
        revisar("10. ana recibe EVENTO de beto desconectado", "EVENTO|ESTADO|beto|0", ana.siguiente());

        // 8. El log tiene la frase del ejemplo con todo en texto visible
        boolean fraseMensaje = false;
        boolean fraseClave = false;
        for (String linea : lineasLog) {
            if (linea.contains("Recibi una peticion para enviar un mensaje de ana a beto. El mensaje dice: hola | con barra")) {
                fraseMensaje = true;
            }
            if (linea.contains("La contrasena es: cacahuate")) {
                fraseClave = true;
            }
        }
        revisar("11. el log muestra el mensaje en texto visible", "true", String.valueOf(fraseMensaje));
        revisar("12. el log muestra la contrasena en texto visible", "true", String.valueOf(fraseClave));

        ana.conexion.cerrar();
        servidor.detener();

        System.out.println();
        System.out.println(fallos == 0 ? "TODO BIEN" : "FALLARON " + fallos + " PRUEBA(S)");
        System.exit(fallos == 0 ? 0 : 1);
    }
}
