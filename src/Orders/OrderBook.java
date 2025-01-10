package Orders;

import Server.NotificationHandler;
import Utility.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class OrderBook {
    private OrderRecord stop;
    private OrderRecord limit;
    private static final String logsFile = "ordersLog.json";
    private static final String orderBookFile = "orderBook.json";
    private transient int maxId = -1;
    private transient List<Trade> logs;

    public void LoadData()
    {
        LoadBook();
        LoadLogs();

        //Find next order id
        for(List<Order> orders : limit.values())
            orders.stream().max(Comparator.comparingInt(order -> order.orderId)).ifPresent(value -> maxId = Math.max(value.orderId, maxId));
        for(List<Order> orders : stop.values())
            orders.stream().max(Comparator.comparingInt(order -> order.orderId)).ifPresent(value -> maxId = Math.max(value.orderId, maxId));
        logs.stream().max(Comparator.comparingInt(order -> order.orderId)).ifPresent(value -> maxId = Math.max(value.orderId, maxId));
        maxId++;
    }

    private void LoadBook()
    {
        File orderBookFile = new File(OrderBook.orderBookFile);
        try{
            Gson gson = new Gson();
            OrderBook savedBook = gson.fromJson(new FileReader(orderBookFile), OrderBook.class);

            stop = savedBook.stop;
            limit = savedBook.limit;

        } catch (Exception e) {
            stop = new OrderRecord();
            limit = new OrderRecord();
        }
    }

    private void LoadLogs()
    {
        File logsFile = new File(OrderBook.logsFile);
        try{
            Gson gson = new Gson();
            TypeToken<List<Trade>> type = new TypeToken<>(){};
            List<Trade> logs = gson.fromJson(new FileReader(logsFile), type);

            this.logs = Collections.synchronizedList(logs);

        } catch (Exception e) {
            this.logs = Collections.synchronizedList(new ArrayList<>());
        }
    }

    public synchronized int InsertNewOrder(String username, OrderType type, OrderAction orderType, int price, int size)
    {
        Order newOrder = new Order(maxId, username, type, price, size);
        int result = newOrder.orderId;
        switch (orderType)
        {
            case limit -> {
                result = executeLimitOrder(newOrder);
            }
            case market -> {
                result = executeMarketOrder(newOrder, false);
            }
            case stop -> {
                result = executeStopOrder(newOrder);
            }
        }
        if(result != -1) maxId++;
        OrderBookModified();
        return result;
    }

    public synchronized int CancelOrder(String username, int orderId)
    {
        int resultLimit = limit.RemoveOrderById(orderId, username);
        int resultStop = stop.RemoveOrderById(orderId, username);

        OrderBookModified();
        return resultLimit != 101 ? resultLimit : resultStop;
    }

    private void OrderBookModified()
    {
        checkForUpdate();
        FileCreator.WriteToFile(orderBookFile, this);
        FileCreator.WriteToFile(logsFile, logs);
    }


    private int executeMarketOrder(Order order, boolean fromStopOrder)
    {
        try{
            calculateMarketOrder(order, fromStopOrder);
            return order.orderId;
        }
        catch (OrderFailedException e)
        {
            return -1;
        }
    }

    private void calculateMarketOrder(Order order, boolean fromStopOrder) throws OrderFailedException
    {
        ArrayList<Trade> logs = new ArrayList<>(); //Users to notify
        List<Order> ordersToInteract = new ArrayList<>();
        List<Order> ordersToRemove = new ArrayList<>();
        OrderType ordersType = order.type == OrderType.ask ? OrderType.bid : OrderType.ask;

        List<Order> availableOrders = limit.get(ordersType);
        //Check if the order can be completed
        int neededSize = order.size;
        for(Order availableOrder : availableOrders) { //Orders already sorted in the correct way
            if(order.username.equals(availableOrder.username)) continue;
            neededSize -= availableOrder.size;
            ordersToInteract.add(availableOrder);
            if(neededSize >= 0) ordersToRemove.add(availableOrder);
            if(neededSize <= 0) break;
        }
        if(neededSize > 0) throw new OrderFailedException(); //There aren't enough bid orders to complete the order

        //Execute the operation that will complete
        for(Order interactionOrder : ordersToInteract)
        {
            int size = Math.min(interactionOrder.size, order.size);
            int price = size * interactionOrder.price;
            logs.add(new Trade(interactionOrder.username, interactionOrder.orderId, interactionOrder.type, OrderAction.limit, size, price));
            logs.add(new Trade(order.username, order.orderId, order.type, fromStopOrder ? OrderAction.stop : OrderAction.market, size, price));

            interactionOrder.size -= size;
            order.size -= size;
        }
        limit.RemoveAll(ordersToRemove, ordersType);

        NotificationHandler.Send(logs);
        AddLogs(logs);
    }

    private int executeStopOrder(Order newOrder) {
        if(checkStopTrigger(newOrder))
            return executeMarketOrder(newOrder, true);
        else{
            stop.AddOrder(newOrder);
            return newOrder.orderId;
        }
    }

    private void checkStopOrder()
    {
        List<Order> ordersToRemove = new ArrayList<>();
        for(List<Order> orders : stop.values())
        {
            for(Order stopOrder : orders)
            {
                if(checkStopTrigger(stopOrder))
                {
                    executeMarketOrder(stopOrder, true);
                    ordersToRemove.add(stopOrder);
                }
            }
            orders.removeAll(ordersToRemove);
        }
    }

    private boolean checkStopTrigger(Order order)
    {
        try{
            if(order.type == OrderType.ask)
            {
                return getMarketPrice(OrderType.bid, order.username) <= order.price; //if market price drops under selling stop price
            }
            else
            {
                return getMarketPrice(OrderType.ask, order.username) >= order.price; //if market price overcomes buying stop price
            }
        }catch (EmptyMarketException _)
        {
            return false; //If market is empty the stop order will fail
        }
    }

    private int getMarketPrice(OrderType type, String username) throws EmptyMarketException
    {
        try{
            return limit.get(type).stream().filter(order -> !order.username.equals(username)).toList().getFirst().price;
        }
        catch (NoSuchElementException _) {
            throw new EmptyMarketException();
        }
    }

    private int executeLimitOrder(Order newOrder) {
        limit.AddOrder(newOrder);
        return newOrder.orderId;
    }

    private void matchLimitOrders()
    {
        HashMap<OrderType, ArrayList<Order>> ordersToRemove = new HashMap<>();
        ordersToRemove.put(OrderType.ask, new ArrayList<>());
        ordersToRemove.put(OrderType.bid, new ArrayList<>());
        ArrayList<Trade> logs = new ArrayList<>();

        for(Order bidOrder : limit.get(OrderType.bid))
        {
            for(Order askOrder : limit.get(OrderType.ask))
            {
                if(askOrder.username.equals(bidOrder.username)) continue;

                int size = 0;
                int price = 0;
                if(bidOrder.price >= askOrder.price)
                {
                    size = Math.min(askOrder.size, bidOrder.size);
                    askOrder.size -= size;
                    bidOrder.size -= size;
                    price = askOrder.timestamp <= bidOrder.timestamp ? askOrder.price : bidOrder.price;
                    if(askOrder.size <= 0) ordersToRemove.get(OrderType.ask).add(askOrder);
                    if(bidOrder.size <= 0) ordersToRemove.get(OrderType.bid).add(bidOrder);
                }
                if(size > 0)
                {
                    logs.add(createLimitLog(askOrder, size, price));
                    logs.add(createLimitLog(bidOrder, size, price));
                }
                if(bidOrder.size <= 0) break;
            }
            limit.RemoveAll(ordersToRemove.get(OrderType.ask), OrderType.ask);
            ordersToRemove.get(OrderType.ask).clear();
        }
        limit.RemoveAll(ordersToRemove.get(OrderType.bid), OrderType.bid);

        NotificationHandler.Send(logs);
        AddLogs(logs);
    }

    private Trade createLimitLog(Order order, int size, int price)
    {
        return new Trade(order.username, order.orderId, order.type, OrderAction.limit, size, size * price);
    }

    private void checkForUpdate()
    {
        checkStopOrder();
        matchLimitOrders();
        checkStopOrder();
    }

    private void AddLogs(ArrayList<Trade> logs) {
        this.logs.addAll(0, logs);
    }
}