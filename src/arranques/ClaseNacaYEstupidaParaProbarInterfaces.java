package arranques;
import com.chat.servidor.ServidorSocket;
import com.chat.servidor.VentanaLog;
import Interfaces.Inicio;
import java.io.IOException;
import javax.swing.SwingUtilities;

public class ClaseNacaYEstupidaParaProbarInterfaces {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaLog log = new VentanaLog();
            log.setVisible(true);

            ServidorSocket servidor = new ServidorSocket(ServidorSocket.PUERTO_DEFECTO, log);
            Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));

            try {
                servidor.iniciar();
            } catch (IOException e) {
                log.registrar("No se pudo abrir el puerto: " + e.getMessage());
            }

            new Inicio().setVisible(true);
        });
    }
}