package Server;

import Messages.*;
import Orders.OrderBook;
import Utility.OrderAction;
import com.google.gson.*;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable{
    private final Socket client;
    private final InetAddress clientAddress;
    private Credentials currentCredentials = null;
    private final OrderBook orderBook;
    public ClientHandler(Socket client, OrderBook orderBook)
    {
        this.client = client;
        this.orderBook = orderBook;
        this.clientAddress = client.getInetAddress();
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
                String response = parseMessage(answer);

                out.writeInt(response.length());
                out.writeBytes(response);
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
        finally {
            CROSSServer.RemoveClient(this);
        }
    }

    private String parseMessage(String receivedMessage)
    {
        ResponseMessage responseMessage;
        Gson gson = new Gson();
        try {
            JsonElement element = JsonParser.parseString(receivedMessage);
            String operation = element.getAsJsonObject().get("operation").getAsString();
            JsonElement values = element.getAsJsonObject().get("values");

            int response;
            String errorMessage;

            switch (operation)
            {
                case "register" -> {
                    if (currentCredentials != null) response = 103;
                    else {
                        Registration registration = gson.fromJson(values, Registration.class);
                        response = LoginHandler.RegisterUser(registration.username, registration.password);
                    }

                    errorMessage = switch (response) {
                        case 100 -> "OK";
                        case 101 -> "invalid password";
                        case 102 -> "username not available";
                        case 103 -> "user currently logged in";
                        default -> "unknown error";
                    };
                    responseMessage = new Response(response, errorMessage);
                }
                case "updateCredentials" -> {
                    if(currentCredentials != null) response = 104;
                    else{
                        UpdateCredentials updateCredentials = gson.fromJson(values, UpdateCredentials.class);
                        response = LoginHandler.UpdateCredentials(updateCredentials.username, updateCredentials.old_password, updateCredentials.new_password);
                    }

                    errorMessage = switch (response) {
                        case 100 -> "OK";
                        case 101 -> "invalid new password";
                        case 102 -> "username/old password mismatch or non-existent username";
                        case 103 -> "new password equal to old one";
                        case 104 -> "user currently logged in";
                        default -> "unknown error";
                    };
                    responseMessage = new Response(response, errorMessage);
                }
                case "login" -> {
                    if(currentCredentials != null) response = 102;
                    else{
                        Login login = gson.fromJson(values, Login.class);
                        response = LoginHandler.LoginUser(login.username, login.password);
                        if(response == 100) currentCredentials = new Credentials(login.username, login.password);
                    }

                    errorMessage = switch (response) {
                        case 100 -> "OK";
                        case 101 -> "username/password mismatch or non-existent username";
                        case 102 -> "user currently logged in";
                        case 103 -> "error updating credentials file";
                        default -> "unknown error";
                    };
                    responseMessage = new Response(response, errorMessage);
                }
                case "logout" -> {
                    response = 101;

                    if(currentCredentials == null) {
                        responseMessage = new Response(response, "user not logged in");
                        break;
                    }

                    Logout logout = gson.fromJson(values, Logout.class);
                    if(!LoginHandler.CheckUsername(logout.username))  errorMessage = "username non existent";
                    else if(!logout.username.equals(currentCredentials.username)) errorMessage = "username/connection mismatch";
                    else {
                        currentCredentials = null;
                        response = 100;
                        errorMessage = "OK";
                    }

                    responseMessage = new Response(response, errorMessage);
                }
                case "insertLimitOrder" -> {
                    if(currentCredentials == null) {
                        responseMessage = new OrderResponse(-1);
                        break;
                    }
                    InsertLimitOrder insertLimitOrder = gson.fromJson(values, InsertLimitOrder.class);
                    int limitOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertLimitOrder.type, OrderAction.limit, insertLimitOrder.price, insertLimitOrder.size);
                    responseMessage = new OrderResponse(limitOrderId);
                }
                case "insertMarketOrder" -> {
                    if(currentCredentials == null) {
                        responseMessage = new OrderResponse(-1);
                        break;
                    }
                    InsertMarketOrder insertMarketOrder = gson.fromJson(values, InsertMarketOrder.class);
                    int marketOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertMarketOrder.type, OrderAction.market, 0, insertMarketOrder.size);
                    responseMessage = new OrderResponse(marketOrderId);
                }
                case "insertStopOrder" -> {
                    if(currentCredentials == null) {
                        responseMessage = new OrderResponse(-1);
                        break;
                    }
                    InsertStopOrder insertStopOrder = gson.fromJson(values, InsertStopOrder.class);
                    int stopOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertStopOrder.type, OrderAction.stop, insertStopOrder.price, insertStopOrder.size);
                    responseMessage = new OrderResponse(stopOrderId);
                }
                case "cancelOrder" -> {
                    if(currentCredentials == null) {
                        responseMessage = new OrderResponse(-1);
                        break;
                    }
                    CancelOrder cancelOrder = gson.fromJson(values, CancelOrder.class);
                    int cancelledResult = orderBook.CancelOrder(currentCredentials.username, cancelOrder.orderId);
                    errorMessage = switch (cancelledResult) {
                        case 100 -> "OK";
                        case 101 -> "order does not exist or has been finalized";
                        case 102 -> "order belongs to a different user";
                        default -> "unknown error";
                    };
                    responseMessage = new Response(Math.min(cancelledResult, 101), errorMessage);
                }
                case "getPriceHistory" -> {
                    if(currentCredentials == null) {
                        responseMessage = new OrderResponse(-1);
                        break;
                    }
                    GetPriceHistory getPriceHistory = gson.fromJson(values, GetPriceHistory.class);
                    responseMessage = new Response(101, "not yet implemented");
                }
                default -> responseMessage = new Response(404, "command not found");
            }
            return gson.toJson(responseMessage);
        }
        catch (JsonSyntaxException e)
        {
            return gson.toJson(new Response(400, "bad request"));
        }
    }

    public InetAddress GetAddressIfLogged(String username)
    {
        if(currentCredentials != null)
        {
            if(currentCredentials.username.equals(username)) return clientAddress;
        }
        return null;
    }
}
