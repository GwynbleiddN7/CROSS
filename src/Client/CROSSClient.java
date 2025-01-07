package Client;

import Messages.*;
import Utility.OrderType;
import Utility.IncorrectParameterException;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.google.gson.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.TimeZone;

public class CROSSClient {
    private static final String configFile = "client.properties";
    private static String host;
    private static int tcp_port;
    private static int udp_port;
    private static final Scanner console = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            readConfig();
        } catch (IOException e) {
            System.out.println("Errore nella lettura del file di configurazione");
            return;
        }

        try(Socket socket = new Socket(host, tcp_port))
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            Gson gson = new Gson();
            NotificationHandler notificationHandler = new NotificationHandler(udp_port);
            Thread notificationThread = new Thread(notificationHandler);
            notificationThread.start();

            System.out.println("Inserisci un comando [format: cmd(param1, param2, ...)]");
            while(true)
            {
                System.out.print(">");
                String input = console.nextLine();

                if(input.equals("EXIT"))
                {
                    out.writeInt(input.length());
                    out.writeBytes(input);
                    out.flush();
                    break;
                }

                MessageType msg = parseInput(input);
                if(msg == null) continue;
                Message<MessageType> message = new Message<>(msg);

                String stringMessage = gson.toJson(message);
                out.writeInt(stringMessage.length());
                out.writeBytes(stringMessage);
                out.flush();

                int size = in.readInt();
                byte[] buff = new byte[size];
                int len = in.read(buff, 0, size);

                String answer = new String(buff, 0, len);
                System.out.println(answer);
            }
            notificationHandler.stopListener();
            out.close();
            in.close();
        } catch (IOException e) {
            System.out.println("Impossibile connettersi al server");
        }
    }

    private static MessageType parseInput(String input)
    {
        String[] parts = input.split("\\(");
        if(parts.length == 0) return null;
        try{
            String[] params = parts[1].replace(")", "").trim().split(", ");
            switch (parts[0].trim())
            {
                case "register":
                    if(params.length != 2) throw new IncorrectParameterException();
                    return new Registration(params[0], params[1]);
                case "updateCredentials":
                    if(params.length != 3) throw new IncorrectParameterException();
                    return new UpdateCredentials(params[0], params[1], params[2]);
                case "login":
                    if(params.length != 2) throw new IncorrectParameterException();
                    return new Login(params[0], params[1]);
                case "logout":
                    if(params.length != 1) throw new IncorrectParameterException();
                    return new Logout(params[0]);
                case "insertLimitOrder":
                    if(params.length != 3) throw new IncorrectParameterException();
                    return new InsertLimitOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2]));
                case "insertMarketOrder":
                    if(params.length != 2) throw new IncorrectParameterException();
                    return new InsertMarketOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]));
                case "insertStopOrder":
                    if(params.length != 3) throw new IncorrectParameterException();
                    return new InsertStopOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2]));
                case "cancelOrder":
                    if(params.length != 1) throw new IncorrectParameterException();
                    return new CancelOrder(Integer.parseInt(params[0]));
                case "getPriceHistory":
                    if(params.length != 1) throw new IncorrectParameterException();
                    Calendar time = Calendar.getInstance();
                    time.set(Calendar.MONTH, Integer.parseInt(params[0]) - 1);
                    time.setTimeZone(TimeZone.getTimeZone("GMT"));
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy");
                    return new GetPriceHistory(sdf.format(time.getTime()));
                default:
                    System.out.println("Comando sconosciuto");
                    break;
            }
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Errore nel tipo di un parametro " + e.toString());
        }
        catch (ArrayIndexOutOfBoundsException | IncorrectParameterException e)
        {
            System.out.println("Errore nel numero dei parametri del comando");
        }
        return null;
    }

    public static void readConfig() throws IOException {
        InputStream input = CROSSClient.class.getResourceAsStream(configFile);
        if(input == null) throw new IOException();

        Properties prop = new Properties();
        prop.load(input);
        tcp_port = Integer.parseInt(prop.getProperty("TCP_port"));
        udp_port = Integer.parseInt(prop.getProperty("UDP_port"));
        host = prop.getProperty("host");
        input.close();
    }
}