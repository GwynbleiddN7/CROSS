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
    private static final ExecutorService pool = Executors.newCachedThreadPool(); //Creo la pool di thread
    private static final List<ClientHandler> clientList = Collections.synchronizedList(new ArrayList<>()); //Creo la lista sincronizzata dei client connessi
    private static final String configFile = "server.properties";
    private static int tcp_port;
    public static int udp_port;
    public static int timeout;

    public static void main(String[] args) {
        try {
            readConfig(); //Provo a leggere le configurazione dal file server.properties
        } catch (IOException e) {
            System.out.println("Errore nella lettura del file di configurazione");
            return;
        }

        LoginHandler.LoadData(); //Carico i dati delle credenziali degli utenti
        OrderBook orderBook = new OrderBook();
        orderBook.LoadData(); //Carico i dati dell'orderBook

        try(ServerSocket serverSocket = new ServerSocket(tcp_port)) //Creo la socket TCP
        {
            System.out.println("Server ready");
            while(true)
            {
                try
                {
                    Socket client = serverSocket.accept(); //Accetto la connessione con un nuovo client
                    ClientHandler handler = new ClientHandler(client, orderBook); //Creo un nuovo Runnable da passare alla thread pool
                    clientList.add(handler); //Aggiungo il nuovo client alla lista di quelli connessi (lista già sincronizzata per le operazioni atomiche)
                    pool.execute(handler); //Eseguo il Runnable su un thread
                }
                catch (RejectedExecutionException e)
                {
                    System.out.println("Impossibile accettare un nuovo client");
                }
                catch (IOException e)
                {
                    System.out.println("Errore nella connessione con un client");
                }
            }
        } catch (IOException e) {
            System.out.println("Errore nella gestione del socket");
        }
    }

    //Funzione che legge la porta da usare per TCP e quella per UDP
    private static void readConfig() throws IOException {
        InputStream input = ServerMain.class.getResourceAsStream(configFile);
        if(input == null) throw new IOException();

        Properties prop = new Properties();
        prop.load(input);
        tcp_port = Integer.parseInt(prop.getProperty("TCP_port"));
        udp_port = Integer.parseInt(prop.getProperty("UDP_port"));
        timeout = Integer.parseInt(prop.getProperty("timeout"));
        input.close();
    }

    //Funzione che rimuove dalla lista un client che è stato chiuso (lista già sincronizzata per le operazioni atomiche)
    public static void RemoveClient(ClientHandler handler)
    {
        clientList.remove(handler);
    }

    //Funzione che cerca l'indirizzo del client connesso e loggato con un certo username, per mandare una notifica tramite UDP
    public static InetSocketAddress GetClientAddressByUsername(String username)
    {
        synchronized (clientList) //Dato che sto iterando sulla lista, prendo il lock
        {
            //Se trovo l'indirizzo lo ritorno, altrimenti ritorno null (Best-Effort)
            for(ClientHandler handler : clientList)
            {
                InetSocketAddress address = handler.GetAddressIfLogged(username);
                if(address != null) return address;
            }
            return null;
        }
    }
}
