package Client;

import Messages.Notification;
import Messages.Trade;
import Utility.OrderType;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class NotificationHandler implements Runnable{
    private final DatagramSocket socket;
    public NotificationHandler()
    {
        try{
            socket = new DatagramSocket(0);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopListener()
    {
        socket.close();
    }

    public int GetPort(){
        return socket.getLocalPort();
    }

    public void run() {
        Gson gson = new Gson();
        while(true)
        {
            DatagramPacket notification = new DatagramPacket(new byte[1024], 1024);
            try {
                socket.receive(notification);
                String text = new String(notification.getData(), 0, notification.getLength());
                Notification answerNotification = gson.fromJson(text, Notification.class);
                System.out.printf("RAW MESSAGE: [%s]\n", text);
                parseNotification(answerNotification);
            } catch (IOException e) {
                System.out.println("Notification service interrupted");
                return;
            }
        }
    }

    private static void parseNotification(Notification notification)
    {
        for(Trade trade : notification.trades)
        {
            System.out.printf("[Notifica ordine %d di tipo %s: hai %s %d BTC per un totale di %d EUR]\n>", trade.orderId, trade.orderType, trade.type == OrderType.ask ? "venduto" : "acquistato", trade.size, trade.price);
        }
    }
}
