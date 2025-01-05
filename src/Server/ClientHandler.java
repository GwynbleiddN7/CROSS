package Server;

import Messages.*;
import com.google.gson.*;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private final Socket client;
    public ClientHandler(Socket client)
    {
        this.client = client;
    }

    public void run() {
        try
        {
            DataOutputStream out;
            DataInputStream in;
            while(true)
            {
                out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                in = new DataInputStream(new BufferedInputStream(client.getInputStream()));

                int size = in.readInt();
                byte[] buff = new byte[size];
                int len = in.read(buff, 0, size);
                String answer = new String(buff, 0, len);

                if(answer.equals("EXIT")) break;
                parseMessage(answer);

                out.writeInt(answer.length());
                out.writeBytes(answer);
                out.flush();
            }
            out.close();
            in.close();
            client.close();
        }
        catch (IOException e)
        {
            System.out.println("Errore nella comunicazione con il client");
        }
    }

    private void parseMessage(String receivedMessage)
    {
        try {
            JsonElement element = JsonParser.parseString(receivedMessage);
            String operation = element.getAsJsonObject().get("operation").getAsString();
            JsonElement values = element.getAsJsonObject().get("values");

            Gson gson = new Gson();
            switch (operation)
            {
                case "register":
                    Registration registration = gson.fromJson(values, Registration.class);
                    break;
                case "updateCredentials":
                    UpdateCredentials updateCredentials = gson.fromJson(values, UpdateCredentials.class);
                    break;
                case "login":
                    Login login = gson.fromJson(values, Login.class);
                    break;
                case "logout":
                    Logout logout = gson.fromJson(values, Logout.class);
                    break;
                case "insertLimitOrder":
                    InsertLimitOrder insertLimitOrder = gson.fromJson(values, InsertLimitOrder.class);
                    break;
                case "insertMarketOrder":
                    InsertMarketOrder insertMarketOrder = gson.fromJson(values, InsertMarketOrder.class);
                    break;
                case "insertStopOrder":
                    InsertStopOrder insertStopOrder = gson.fromJson(values, InsertStopOrder.class);
                    break;
                case "cancelOrder":
                    CancelOrder cancelOrder = gson.fromJson(values, CancelOrder.class);
                    break;
                case "getPriceHistory":
                    GetPriceHistory gt = gson.fromJson(values, GetPriceHistory.class);
                    break;
                default:
                    break;
            }
        }
        catch (JsonSyntaxException e)
        {
        }
    }
}
