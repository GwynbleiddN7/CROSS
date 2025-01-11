package Server;

import ServerToClient.Notification;
import Orders.Trade;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

//Classe che gestisce le notifiche da mandare tramite UDP
public class NotificationHandler {
    public static void Send(ArrayList<Trade> trades)
    {
        ArrayList<String> usernames = new ArrayList<>();

        //Trova tutti gli utenti che hanno completato un trade
        for(Trade trade: trades)
        {
            if(!usernames.contains(trade.username)) usernames.add(trade.username);
        }

        //Raggruppa gli utenti per mandare le relative notifiche di trade
        for(String username: usernames)
        {
            InetSocketAddress address = ServerMain.GetClientAddressByUsername(username);
            if(address == null) continue; //Best-Effort
            List<Trade> tradesPerUser = trades.stream().filter(order -> order.username.equals(username)).toList(); //Trade relativi all'utente in questione
            Notification notification = new Notification(tradesPerUser); //Crea una nuova notifica
            SendDatagram(notification, address); //Manda la notifica all'utente
        }
    }

    //Funzione per mandare il datagram tramite UDP
    private static void SendDatagram(Notification notification, InetSocketAddress address)
    {
        try(DatagramSocket socket = new DatagramSocket()){
            try{
                Gson gson = new Gson();
                String stringMessage = gson.toJson(notification); //Serializza la notifica
                byte[] msg = stringMessage.getBytes();
                DatagramPacket request = new DatagramPacket(msg, msg.length, address); //Crea il datagram con la notifica
                socket.send(request); //Manda il datagram tramite la socket
            }
            catch (IOException e) {
                System.out.println("Errore di comunicazione con il server");
            }
        } catch (SocketException e) {
            System.out.println("Errore nella gestione del socket");
        }
    }
}
