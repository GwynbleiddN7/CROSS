package Server;

import Orders.OrderBook;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class ServerMain {
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final List<ClientHandler> clientList = Collections.synchronizedList(new ArrayList<>());
    private static final String configFile = "server.properties";
    private static int tcp_port;
    public static int udp_port;

    public static void main(String[] args) {
        try {
            readConfig();
        } catch (IOException e) {
            System.out.println("Errore nella lettura del file di configurazione");
            return;
        }

        LoginHandler.LoadData();
        OrderBook orderBook = new OrderBook();
        orderBook.LoadData();

        try(ServerSocket serverSocket = new ServerSocket(tcp_port))
        {
            System.out.println("Server ready");
            while(true)
            {
                try
                {
                    Socket client = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(client, orderBook);
                    clientList.add(handler);
                    pool.execute(handler);
                }
                catch (RejectedExecutionException e)
                {
                    System.out.println("Impossibile accettare un nuovo client");
                }
                catch (IOException e)
                {
                    System.out.println("Errore nella connessione con il client");
                    break;
                }

            }
        } catch (IOException e) {
            System.out.println("Errore nella gestione del socket");
        }
    }
    private static void readConfig() throws IOException {
        InputStream input = ServerMain.class.getResourceAsStream(configFile);
        if(input == null) throw new IOException();

        Properties prop = new Properties();
        prop.load(input);
        tcp_port = Integer.parseInt(prop.getProperty("TCP_port"));
        udp_port = Integer.parseInt(prop.getProperty("UDP_port"));
        input.close();
    }

    public static void RemoveClient(ClientHandler handler)
    {
        clientList.remove(handler);
    }

    public static InetSocketAddress GetClientAddressByUsername(String username)
    {
        synchronized (clientList)
        {
            for(ClientHandler handler : clientList)
            {
                InetSocketAddress address = handler.GetAddressIfLogged(username);
                if(address != null) return address;
            }
            return null;
        }
    }
}
