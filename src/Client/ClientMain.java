package Client;

import ClientToServer.*;
import ServerToClient.OrderResponse;
import ServerToClient.PriceHistory;
import ServerToClient.PriceResponse;
import ServerToClient.StandardResponse;
import Utility.Operation;
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

public class ClientMain {
    private static final String configFile = "client.properties";
    private static String host;
    private static int tcp_port;
    private final static Gson gson = new Gson();
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


            NotificationHandler notificationHandler = new NotificationHandler();
            out.writeInt(notificationHandler.GetPort());

            Thread notificationThread = new Thread(notificationHandler);
            notificationThread.start();

            System.out.println("Inserisci un comando [format: cmd(param1, param2, ...)]");
            while(true)
            {
                System.out.print(">");
                String input = console.nextLine();

                MessageType msg = parseInput(input);
                if(msg == null) continue;
                Message<MessageType> message = new Message<>(msg);

                String stringMessage = gson.toJson(message);
                out.writeInt(stringMessage.length());
                out.writeBytes(stringMessage);
                out.flush();

                if(msg.getOperation() == Operation.exit) break;

                while (true)
                {
                    int size = in.readInt();
                    byte[] buff = new byte[size];
                    int len = in.read(buff, 0, size);
                    String answer = new String(buff, 0, len);

                    boolean endingMessage = parseOutput(answer);
                    if(endingMessage) break;
                }
            }
            notificationHandler.stopListener();
            out.close();
            in.close();
        } catch (IOException e) {
            System.out.println("Impossibile connettersi al server o avviare il servizio di notifiche");
        }
    }

    private static MessageType parseInput(String input)
    {
        String[] parts = input.split("\\(");
        if(parts.length == 0) return null;
        try{
            String[] params = new String[0];
            if(parts.length > 1) params = parts[1].replace(")", "").replace(", ", ",").trim().split(",");
            int paramNum = params.length;
            switch (Operation.valueOf(parts[0].trim()))
            {
                case Operation.exit:
                    if(paramNum != 0) throw new IncorrectParameterException();
                    return new Exit();
                case Operation.register:
                    if(paramNum != 2) throw new IncorrectParameterException();
                    return new Registration(params[0], params[1]);
                case Operation.updateCredentials:
                    if(paramNum != 3) throw new IncorrectParameterException();
                    return new UpdateCredentials(params[0], params[1], params[2]);
                case Operation.login:
                    if(paramNum != 2) throw new IncorrectParameterException();
                    return new Login(params[0], params[1]);
                case Operation.logout:
                    if(paramNum != 0) throw new IncorrectParameterException();
                    return new Logout();
                case Operation.insertLimitOrder:
                    if(paramNum != 3) throw new IncorrectParameterException();
                    return new InsertLimitOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2]));
                case Operation.insertMarketOrder:
                    if(paramNum != 2) throw new IncorrectParameterException();
                    return new InsertMarketOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]));
                case Operation.insertStopOrder:
                    if(paramNum != 3) throw new IncorrectParameterException();
                    return new InsertStopOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2]));
                case Operation.cancelOrder:
                    if(paramNum != 1) throw new IncorrectParameterException();
                    return new CancelOrder(Integer.parseInt(params[0]));
                case Operation.getPriceHistory:
                    if(paramNum != 1) throw new IncorrectParameterException();
                    Calendar time = Calendar.getInstance();
                    time.set(Calendar.MONTH, Integer.parseInt(params[0]) - 1);
                    time.set(Calendar.YEAR, 2024);
                    time.setTimeZone(TimeZone.getTimeZone("GMT"));
                    SimpleDateFormat sdf = new SimpleDateFormat("MMyyyy");
                    return new GetPriceHistory(sdf.format(time.getTime()));
                default:
                    System.out.println("Comando sconosciuto");
                    break;
            }
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Comando sconosciutp");
        }
        catch (ArrayIndexOutOfBoundsException | IncorrectParameterException e)
        {
            System.out.println("Errore nei parametri del comando");
        }
        return null;
    }

    private static boolean parseOutput(String answer)
    {
        JsonElement element = JsonParser.parseString(answer);
        if(element.getAsJsonObject().get("data") != null)
        {
            PriceResponse priceResponse = gson.fromJson(answer, PriceResponse.class);
            for(PriceHistory history: priceResponse.data)
            {
                System.out.printf("Day %d: opening: %.2f USD, closing: %.2f USD, max: %.2f USD, min: %.2f USD\n", history.day, history.openingPrice / 1000.f, history.closingPrice / 1000.f, history.maxPrice / 1000.f, history.minPrice / 1000.f);
            }
            return false;
        }
        else if(element.getAsJsonObject().get("orderId") != null)
        {
            OrderResponse orderResponse = gson.fromJson(answer, OrderResponse.class);
            if(orderResponse.orderId == -1) System.out.println("Non è stato possibile effettuare l'ordine");
            else System.out.printf("Ordine %d piazzato con successo\n", orderResponse.orderId);
        }
        else if(element.getAsJsonObject().get("response") != null)
        {
            StandardResponse responseMessage = gson.fromJson(answer, StandardResponse.class);
            System.out.printf("Risposta %d: %s\n", responseMessage.response, responseMessage.errorMessage);
        }
        System.out.printf("RAW MESSAGE: [%s]\n", answer);
        return true;
    }

    private static void readConfig() throws IOException {
        InputStream input = ClientMain.class.getResourceAsStream(configFile);
        if(input == null) throw new IOException();

        Properties prop = new Properties();
        prop.load(input);
        tcp_port = Integer.parseInt(prop.getProperty("TCP_port"));
        host = prop.getProperty("host");
        input.close();
    }
}