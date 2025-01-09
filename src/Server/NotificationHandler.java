package Server;

import Messages.Notification;
import Messages.Trade;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationHandler {
    public static void Send(ArrayList<Trade> trades)
    {
        ArrayList<String> usernames = new ArrayList<>();

        //Find all users
        for(Trade trade: trades)
        {
            if(!usernames.contains(trade.username)) usernames.add(trade.username);
        }

        //Group notification per user
        for(String username: usernames)
        {
            InetSocketAddress address = ServerMain.GetClientAddressByUsername(username);
            if(address == null) continue; //Best-Effort
            List<Trade> tradesPerUser = trades.stream().filter(order -> order.username.equals(username)).toList();
            Notification notification = new Notification(tradesPerUser);
            Send(notification, address);
        }
    }
    private static void Send(Notification notification, InetSocketAddress address)
    {
        try(DatagramSocket socket = new DatagramSocket()){
            try{
                Gson gson = new Gson();
                String stringMessage = gson.toJson(notification);
                byte[] msg = stringMessage.getBytes();
                DatagramPacket request = new DatagramPacket(msg, msg.length, address);
                socket.send(request);
            }
            catch (IOException e) {
                System.out.println("Error communicating with Server");
            }
        } catch (SocketException e) {
            System.out.println("Error with the socket");
        }
    }
}
