package Client;

import ServerToClient.Notification;
import Orders.Trade;
import Utility.OrderType;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

//Classe Runnable che si occupa di ricevere le notifiche UDP dal server in modo asincrono
public class NotificationHandler implements Runnable{
    private final DatagramSocket socket; //Socket per la ricezione dei datagram UDP
    public NotificationHandler() throws SocketException
    {
        socket = new DatagramSocket(0); //Uso una porta qualsiasi per ricevere i dati
    }

    //Funzione che interrompe il servizio di notifiche
    public void stopListener()
    {
        socket.close();
    }

    //Funzione che ritorna la porta che è stata scelta dalla socket per eseguire il binding della connessione
    public int GetPort(){
        return socket.getLocalPort();
    }

    //Funzione asincrona che si occupa di ricevere le notifiche
    public void run() {
        Gson gson = new Gson();
        while(true)
        {
            DatagramPacket notification = new DatagramPacket(new byte[1024], 1024); //Accetto datagram da 1024 byte, abbastanza grande da contenere un numero statisticamente possibile di trade conclusi (comunque best-effort)
            try {
                socket.receive(notification); //Ricevo il datagram
                String text = new String(notification.getData(), 0, notification.getLength()); //Lo converto in testo
                Notification answerNotification = gson.fromJson(text, Notification.class); //Lo de-serializzo nella classe di appartenenza
                System.out.printf("RAW MESSAGE: [%s]\n", text); //Stampo il testo RAW a scopo informativo per il progetto
                parseNotification(answerNotification); //Stampo il testo per l'utente
            } catch (IOException e) {
                System.out.println("Servizio di notifiche interrotto");
                return;
            }
        }
    }

    //Funzione che scandisce la lista dei trade nella notifica e li scrive a schermo
    private static void parseNotification(Notification notification)
    {
        for(Trade trade : notification.trades)
        {
            //Divido per 1000.f per mostrare all'utente i valori effettivi della transazione
            System.out.printf("[Notifica ordine %d di tipo %s: hai %s %.4f BTC per un totale di %.2f USD]\n>", trade.orderId, trade.orderType, trade.type == OrderType.ask ? "venduto" : "acquistato", trade.size/1000.f, trade.price/1000.f);
        }
    }
}
