package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class NotificationHandler implements Runnable{
    DatagramSocket socket;
    public NotificationHandler(int port)
    {
        try{
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopListener()
    {
        socket.close();
    }

    public void run() {
        while(true)
        {
            DatagramPacket notification = new DatagramPacket(new byte[1024], 1024);
            try {
                socket.receive(notification);
                String text = new String(notification.getData(), 0, notification.getLength());
                System.out.println("New notification: " + text);
            } catch (IOException e) {
                System.out.println("Notification service interrupted");
                return;
            }
        }
    }
}
