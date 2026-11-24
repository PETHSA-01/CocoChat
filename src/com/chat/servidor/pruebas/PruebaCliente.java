package com.chat.servidor.pruebas;

import com.chat.cliente.ConexionServidor;
import com.chat.common.Peticion;
import com.chat.common.Protocolo;
import com.chat.servidor.ServidorSocket;

/**
 * Cliente de prueba para el servidor REAL.
 *
 * Pasos:
 *   1. Ejecuten ServidorSocket (Run File) y dejen abierta su ventana de log.
 *   2. Ejecuten esta clase (Run File).
 *
 * Se conecta a localhost, manda un LOGIN y un MENSAJE e imprime lo que
 * responde el servidor. En la ventana de log deben aparecer las lineas
 * "Se conecto un cliente...", "Recibi una peticion de inicio de sesion de ana..."
 * y "Envie a ...".
 *
 * Mientras ManejadorAuth no este registrado, el servidor contesta
 * ERROR|LOGIN|Accion desconocida o todavia no disponible: es lo esperado.
 *
 * Para probar desde OTRA computadora, cambien "localhost" por la IP que
 * muestra la ventana de log (por ejemplo 192.168.1.78).
 */
public class PruebaCliente {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int puerto = ServidorSocket.PUERTO_DEFECTO;

        ConexionServidor conexion = new ConexionServidor();
        conexion.setEscucha(new ConexionServidor.Escucha() {
            @Override
            public void alRecibir(Peticion p) {
                System.out.println("[cliente] llego: " + p + "  (accion = " + p.getAccion() + ")");
            }

            @Override
            public void alDesconectar() {
                System.out.println("[cliente] se cerro la conexion");
            }
        });

        System.out.println("[cliente] conectando a " + host + ":" + puerto + " ...");
        conexion.conectar(host, puerto);
        System.out.println("[cliente] conectado");

        conexion.enviar(Protocolo.LOGIN, "ana", "cacahuate");
        Thread.sleep(500);
        conexion.enviar(Protocolo.MENSAJE, "ana", "beto", Protocolo.TEMPORAL, "hola");
        Thread.sleep(1500);

        conexion.cerrar();
        Thread.sleep(300);
        System.out.println("[cliente] fin de la prueba");
    }
}
