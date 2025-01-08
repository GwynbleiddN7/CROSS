package Orders;

import Messages.Trade;
import Server.NotificationHandler;
import Utility.FileCreator;
import Utility.OrderAction;
import Utility.OrderFailedException;
import Utility.OrderType;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class OrderBook {
    private OrderRecord stop;
    private OrderRecord limit;
    private static final String pathFile = "orderBook.json";

    public OrderBook()
    {
        File orderBookFile = new File(pathFile);
        try{
            Gson gson = new Gson();
            OrderBook savedBook = gson.fromJson(new FileReader(orderBookFile), OrderBook.class);

            stop = new OrderRecord(savedBook.stop);
            limit = new OrderRecord(savedBook.limit);

        } catch (Exception _) {
            stop = new OrderRecord();
            limit = new OrderRecord();
        }
    }

    public synchronized int InsertNewOrder(String username, OrderType type, OrderAction orderType, int price, int size)
    {
        Order newOrder = new Order(username, type, price, size);
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
        checkForUpdate();
        FileCreator.WriteToFile(pathFile, this);
        return result;
    }

    public synchronized int CancelOrder(String username, int orderId)
    {
        int result = limit.RemoveOrderById(orderId, username);
        if(result != 101) return result;

        result = stop.RemoveOrderById(orderId, username);
         return result;
    }


    private synchronized int executeMarketOrder(Order order, boolean fromStopOrder)
    {
        ArrayList<Trade> logs = new ArrayList<>(); //Users to notify

        try{
            calculateMarketOrder(order, logs);

            Trade log = new Trade(order.username, order.orderId, order.type, fromStopOrder ? OrderAction.stop : OrderAction.market, order.size, order.price, new Date().getTime());
            logs.add(log);
            NotificationHandler.Send(logs);

            return order.orderId;
        }
        catch (OrderFailedException e)
        {
            return -1;
        }
    }

    private synchronized void calculateMarketOrder(Order order, ArrayList<Trade> logs) throws OrderFailedException
    {
        List<Order> ordersToRemove = new ArrayList<>();
        List<Order> availableOrders = limit.get(order.type == OrderType.ask ? OrderType.bid : OrderType.ask);
        //Check if the order can be completed
        int neededSize = order.size;
        for(Order availableOrder : availableOrders) {
            if(order.username.equals(availableOrder.username)) continue;
            neededSize -= availableOrder.size;
        }
        if(neededSize > 0) throw new OrderFailedException(); //There aren't enough bid orders to complete the order

        //Execute the operation that will complete
        for(Order availableOrder : availableOrders)
        {
            if(order.username.equals(availableOrder.username)) continue;

            int size = Math.min(availableOrder.size, order.size);
            order.price += size * availableOrder.price;

            Trade log = new Trade(availableOrder.username, availableOrder.orderId, availableOrder.type, OrderAction.limit, size, size * availableOrder.price, new Date().getTime());
            logs.add(log); //Log for Notification and for JSON Dump

            availableOrder.size -= size;
            if(availableOrder.size <= 0) {
                ordersToRemove.add(availableOrder); //Flag to remove if completed
            }
        }
        limit.RemoveAll(ordersToRemove, order.type);
    }

    private synchronized int executeStopOrder(Order newOrder) {
        if(comparePrice(newOrder))
            return convertStopOrder(newOrder);
        else{
            stop.AddOrder(newOrder);
            return newOrder.orderId;
        }
    }

    private synchronized int convertStopOrder(Order newOrder)
    {
        newOrder.price = 0;
        return executeMarketOrder(newOrder, true);
    }

    private synchronized void checkStopOrder()
    {
        List<Order> ordersToRemove = new ArrayList<>();
        for(List<Order> orders : stop.values())
        {
            for(Order stopOrder : orders)
            {
                if(comparePrice(stopOrder))
                {
                    convertStopOrder(stopOrder);
                    ordersToRemove.add(stopOrder);
                }
            }
            orders.removeAll(ordersToRemove);
        }
    }

    private synchronized void checkForUpdate()
    {
        checkStopOrder();
        matchLimitOrders();
    }

    private synchronized int executeLimitOrder(Order newOrder) {
        limit.AddOrder(newOrder);
        return newOrder.orderId;
    }

    private synchronized void matchLimitOrders()
    {

    }

    private synchronized int getMarketPrice(OrderType type)
    {
        try{
            if(type == OrderType.ask)
                return limit.get(OrderType.bid).getFirst().price;
            else
                return limit.get(OrderType.ask).getFirst().price;
        }
        catch (NoSuchElementException _) {
            return type == OrderType.ask ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
    }

    private synchronized boolean comparePrice(Order order)
    {
        if(order.type == OrderType.ask)
        {
            return order.price >= getMarketPrice(order.type);
        }
        else
        {
            return order.price <= getMarketPrice(order.type);
        }
    }
}