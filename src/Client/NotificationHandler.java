package Client;

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
