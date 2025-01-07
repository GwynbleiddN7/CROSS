package Orders;

import Messages.Trade;
import Server.NotificationHandler;
import Utility.OrderAction;
import Utility.OrderFailedException;
import Utility.OrderType;
import java.util.ArrayList;
import java.util.Date;

public class OrderBook {
    public OrderRecord stop;
    public OrderRecord limit;

    public synchronized int InsertNewOrder(String username, OrderType type, OrderAction orderType, int price, int size)
    {
        Order newOrder = new Order(username, type, price, size);
        switch (orderType)
        {
            case limit -> {

            }
            case market -> {
                return ExecuteMarkedOrder(newOrder);
            }
            case stop -> {
            }
        }

        return -1;
    }

    public synchronized int CancelOrder(String username, int orderId)
    {
        int result = removeFromList(orderId, username, limit.ask);
        if(result != 101) return result;

        result = removeFromList(orderId, username, limit.bid);
        if(result != 101) return result;

        result = removeFromList(orderId, username, stop.ask);
        if(result != 101) return result;

        result = removeFromList(orderId, username, stop.bid);
        return result;
    }

    private synchronized int removeFromList(int orderId, String username, ArrayList<Order> orderList)
    {
        for(Order order : orderList)
        {
            if(order.orderId == orderId)
            {
                if(username.equals(order.username))
                {
                    orderList.remove(order);
                    return 100;
                }
                else return 102;
            }
        }
        return 101;
    }

    public void CheckOrders()
    {

    }

    public void CheckStopOrders()
    {
        int currentPrice = GetMarketPrice();
        for(Order ask : stop.ask)
        {
            if(ask.price >= currentPrice)
            {

            }
        }
    }

    public synchronized int ExecuteMarkedOrder(Order order)
    {
        ArrayList<Trade> logs = new ArrayList<>(); //Users to notify
        ArrayList<Order> ordersToRemove = new ArrayList<>(); //Orders to remove after the operations

        try{
            calculateMarkedOrder(order, order.type == OrderType.ask ? limit.bid : limit.ask, ordersToRemove, logs);

            Trade log = new Trade(order.username, order.orderId, order.type, OrderAction.market, order.size, order.price, new Date().getTime());
            logs.add(log);

            //Finalize operation
            (order.type == OrderType.ask ? limit.bid : limit.ask).removeAll(ordersToRemove);
            NotificationHandler.Send(logs);

            return order.orderId;
        }
        catch (OrderFailedException e)
        {
            return -1;
        }
    }

    private synchronized void calculateMarkedOrder(Order order, ArrayList<Order> orderList, ArrayList<Order> ordersToRemove, ArrayList<Trade> logs) throws OrderFailedException
    {
        //Check if the order can be completed
        int neededSize = order.size;
        for(Order availableOrder : orderList) {
            if(order.username.equals(availableOrder.username)) continue;
            neededSize -= availableOrder.size;
        }
        if(neededSize > 0) throw new OrderFailedException(); //There aren't enough bid orders to complete the order

        //Execute the operation that will complete
        for(Order availableOrder : orderList)
        {
            if(order.username.equals(availableOrder.username)) continue;

            int size = Math.min(availableOrder.size, order.size);
            order.price += size * availableOrder.price;

            Trade log = new Trade(availableOrder.username, availableOrder.orderId, availableOrder.type, OrderAction.limit, size, availableOrder.price * availableOrder.price, new Date().getTime());
            logs.add(log); //Log for Notification and for JSON Dump

            availableOrder.size -= size;
            if(availableOrder.size <= 0) {
                ordersToRemove.add(availableOrder); //Flag to remove if completed
            }
        }
    }

    public synchronized int GetMarketPrice()
    {
        return -1;
    }
}