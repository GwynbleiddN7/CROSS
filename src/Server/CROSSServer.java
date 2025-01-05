package Server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class CROSSServer {
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final String configFile = "server.properties";
    private static int port;

    public static void main(String[] args) {
        try {
            readConfig();
        } catch (IOException e) {
            System.out.println("Errore nella lettura del file di configurazione");
            return;
        }

        try(ServerSocket serverSocket = new ServerSocket(port))
        {
            while(true)
            {
                try
                {
                    Socket client = serverSocket.accept();
                    pool.execute(new ClientHandler(client));
                }
                catch (SocketException e)
                {
                    System.out.println("Errore nella connessione con il client");
                    break;
                }
                catch (RejectedExecutionException e)
                {
                    System.out.println("Capacità massima raggiunta");
                }

            }
        } catch (IOException e) {
            System.out.println("Errore nella gestione del socket");
        }
    }
    public static void readConfig() throws IOException {
        InputStream input = CROSSServer.class.getResourceAsStream(configFile);
        if(input == null) throw new IOException();

        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        input.close();
    }
}
