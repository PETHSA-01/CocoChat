package com.chat.cliente.pruebas;

import com.chat.cliente.ConexionServidor;
import com.chat.common.Peticion;
import com.chat.common.Protocolo;
import com.formdev.flatlaf.FlatDarkLaf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.UIManager;

/**
 * Prueba ConexionServidor SIN esperar al servidor real del integrante 1.
 * Levanta un "servidor de mentira" en esta misma computadora que:
 *   - responde OK|LOGIN a cualquier LOGIN
 *   - y además manda un evento EVENTO|ESTADO|beto|1
 *
 * En NetBeans: clic derecho en este archivo -> Run File (Shift+F6).
 */
public class PruebaConexion {

    public static void main(String[] args) throws Exception {
        
        
        int puerto = 5050;
        CountDownLatch recibidos = new CountDownLatch(2);

        // ---------- Servidor de mentira ----------
        ServerSocket servidor = new ServerSocket(puerto);
        Thread hiloServidor = new Thread(() -> {
            try (Socket cliente = servidor.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream(), StandardCharsets.UTF_8), true)) {
                String linea = in.readLine();
                System.out.println("[servidor de prueba] recibí: " + linea);
                Peticion p = Protocolo.leer(linea);
                if (Protocolo.LOGIN.equals(p.getAccion())) {
                    out.println(Protocolo.ok(Protocolo.LOGIN));
                    out.println(Protocolo.evento(Protocolo.ESTADO, "beto", Protocolo.CONECTADO));
                }
                Thread.sleep(300);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        hiloServidor.start();

        // ---------- Cliente real ----------
        ConexionServidor conexion = new ConexionServidor();
        conexion.setEscucha(new ConexionServidor.Escucha() {
            @Override
            public void alRecibir(Peticion p) {
                System.out.println("[cliente] llegó: " + p + "  (acción = " + p.getAccion() + ")");
                recibidos.countDown();
            }

            @Override
            public void alDesconectar() {
                System.out.println("[cliente] se cerró la conexión");
            }
        });
        conexion.conectar("localhost", puerto);
        conexion.enviar(Protocolo.LOGIN, "ana", "cacahuate");

        boolean ok = recibidos.await(3, TimeUnit.SECONDS);
        System.out.println(ok ? "\nTodo bien: se envió y se recibió." : "\nFALLA: no llegaron las respuestas.");
        conexion.cerrar();
        servidor.close();
    }
}
