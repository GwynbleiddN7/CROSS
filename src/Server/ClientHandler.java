package Server;

import ClientToServer.*;
import Orders.OrderBook;
import ServerToClient.*;
import Utility.Operation;
import Utility.OrderAction;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

public class ClientHandler implements Runnable{
    private final Socket client;
    private final InetSocketAddress udpSocket;
    private final OrderBook orderBook;
    private final DataOutputStream out;
    private final DataInputStream in;

    private Credentials currentCredentials = null;
    public ClientHandler(Socket client, OrderBook orderBook) throws IOException {
        this.client = client;
        this.orderBook = orderBook;

        this.out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(client.getInputStream()));

        int udpPort = in.readInt();
        this.udpSocket = new InetSocketAddress(this.client.getInetAddress(), udpPort);

        this.client.setSoTimeout(1000 * 5 * 60); // timeout 5 minutes
    }

    public void run() {
        try
        {
            while(true)
            {
                try
                {
                    int size = in.readInt();
                    byte[] buff = new byte[size];
                    int len = in.read(buff, 0, size);

                    String answer = new String(buff, 0, len);
                    String response = parseMessage(answer);
                    if(response.equals("exit")) break;

                    out.writeInt(response.length());
                    out.writeBytes(response);
                    out.flush();
                }
                catch (SocketTimeoutException _)
                {
                    currentCredentials = null;
                }
            }
            out.close();
            in.close();
            client.close();
        }
        catch (IOException _)
        {
            System.out.println("Errore nella comunicazione con il client");
        }
        finally {
            currentCredentials = null;
            ServerMain.RemoveClient(this);
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

            switch (Operation.valueOf(operation))
            {
                case Operation.exit -> {
                    return "exit";
                }
                case Operation.register -> {
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
                    responseMessage = new StandardResponse(response, errorMessage);
                }
                case Operation.updateCredentials -> {
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
                    responseMessage = new StandardResponse(response, errorMessage);
                }
                case Operation.login -> {
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
                    responseMessage = new StandardResponse(response, errorMessage);
                }
                case Operation.logout -> {
                    if(currentCredentials == null) response = 101;
                    else  response = 100;
                    currentCredentials = null;
                    errorMessage = switch (response) {
                        case 100 -> "OK";
                        case 101 -> "user not logged in";
                        default -> "unknown error";
                    };

                    responseMessage = new StandardResponse(response, errorMessage);
                }
                case Operation.insertLimitOrder -> {
                    if(currentCredentials == null) {
                        responseMessage = new OrderResponse(-1);
                        break;
                    }
                    InsertLimitOrder insertLimitOrder = gson.fromJson(values, InsertLimitOrder.class);
                    int limitOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertLimitOrder.type, OrderAction.limit, insertLimitOrder.price, insertLimitOrder.size);
                    responseMessage = new OrderResponse(limitOrderId);
                }
                case Operation.insertMarketOrder -> {
                    if(currentCredentials == null) {
                        responseMessage = new OrderResponse(-1);
                        break;
                    }
                    InsertMarketOrder insertMarketOrder = gson.fromJson(values, InsertMarketOrder.class);
                    int marketOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertMarketOrder.type, OrderAction.market, 0, insertMarketOrder.size);
                    responseMessage = new OrderResponse(marketOrderId);
                }
                case Operation.insertStopOrder -> {
                    if(currentCredentials == null) {
                        responseMessage = new OrderResponse(-1);
                        break;
                    }
                    InsertStopOrder insertStopOrder = gson.fromJson(values, InsertStopOrder.class);
                    int stopOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertStopOrder.type, OrderAction.stop, insertStopOrder.price, insertStopOrder.size);
                    responseMessage = new OrderResponse(stopOrderId);
                }
                case Operation.cancelOrder -> {
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
                    responseMessage = new StandardResponse(Math.min(cancelledResult, 101), errorMessage);
                }
                case Operation.getPriceHistory -> {
                    if(currentCredentials == null) {
                        responseMessage = new StandardResponse(101, "user not logged in");
                        break;
                    }
                    GetPriceHistory getPriceHistory = gson.fromJson(values, GetPriceHistory.class);
                    boolean scanResult = handlePurchaseHistory(getPriceHistory);

                    if(scanResult) responseMessage = new StandardResponse(100, "history scan complete");
                    else responseMessage = new StandardResponse(102, "history scan failed");
                }
                default -> responseMessage = new StandardResponse(404, "command not found");
            }
            return gson.toJson(responseMessage);
        }
        catch (IllegalArgumentException _)
        {
            return gson.toJson(new StandardResponse(404, "command not found"));
        }
        catch (JsonSyntaxException _)
        {
            return gson.toJson(new StandardResponse(400, "bad request"));
        }
    }

    private boolean handlePurchaseHistory(GetPriceHistory priceHistory) {
        try {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("MMyyyy")
                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                    .toFormatter();
            LocalDate date = LocalDate.parse(priceHistory.month, formatter);
            LocalDateTime gmtDate = date.atStartOfDay();

            long endingBound = Date.from(gmtDate.plusMonths(1).toInstant(ZoneOffset.UTC)).getTime();
            long lowerBound = Date.from(gmtDate.toInstant(ZoneOffset.UTC)).getTime();
            long upperBound = lowerBound;
            long defaultTime = new Date().getTime();
            int day = 0;

            ArrayList<PriceHistory> prices = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();

            JsonReader reader = new JsonReader(new FileReader("storicoOrdini.json"));
            reader.beginObject();
            while (reader.hasNext()){
                String name = reader.nextName();
                if ("trades".equals(name)){
                    reader.beginArray();

                    PriceHistory currentPrice = null;

                    boolean done = false;
                    while (reader.hasNext()){
                        if(done) {
                            reader.skipValue();
                            continue;
                        }
                        reader.beginObject();
                        int price = 0;
                        long time = defaultTime;
                        while (reader.hasNext()){
                            String obj = reader.nextName();
                            if ("price".equals(obj)) price = reader.nextInt();
                            else if("timestamp".equals(obj)) time = reader.nextLong() * 1000;
                            else reader.skipValue();
                        }

                        if(time >= lowerBound)
                        {
                            if(time < endingBound)
                            {
                                if(time >= upperBound)
                                {
                                    if(prices.size() == 8)
                                    {
                                        sendPriceHistory(prices, threads);
                                        prices.clear();
                                    }

                                    day++;
                                    lowerBound = upperBound;
                                    gmtDate = gmtDate.plusDays(1);
                                    upperBound = Date.from(gmtDate.toInstant(ZoneOffset.UTC)).getTime();

                                    currentPrice = new PriceHistory();
                                    currentPrice.day = day;
                                    prices.add(currentPrice);
                                }
                                if(currentPrice != null)
                                {
                                    if(currentPrice.openingPrice == -1) currentPrice.openingPrice = price;
                                    currentPrice.closingPrice = price;
                                    currentPrice.maxPrice = Math.max(price, currentPrice.maxPrice);
                                    currentPrice.minPrice = Math.min(price, currentPrice.minPrice);
                                }
                            }
                        }
                        reader.endObject();
                        if(time >= endingBound) {
                            sendPriceHistory(prices, threads);
                            done = true;
                        }
                    }
                    reader.endArray();
                } else reader.skipValue();
            }
            reader.endObject();
            reader.close();

            for(Thread t : threads) t.join();
        } catch (IOException | InterruptedException _) { return false; }
        return true;
    }

    private void sendPriceHistory(ArrayList<PriceHistory> prices, List<Thread> threads)
    {
        if(prices.isEmpty()) return;
        final PriceResponse priceResponse = new PriceResponse(new ArrayList<>(prices));
        Thread thread = new Thread(() -> {
            Gson gson = new Gson();
            String msg = gson.toJson(priceResponse);
            synchronized (out)
            {
                try {
                    out.writeInt(msg.length());
                    out.writeBytes(msg);
                    out.flush();
                } catch (IOException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
        threads.add(thread);
    }

    public InetSocketAddress GetAddressIfLogged(String username)
    {
        if(currentCredentials != null)
        {
            if(currentCredentials.username.equals(username)) return udpSocket;
        }
        return null;
    }
}
