package Orders;

import Utility.OrderType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OrderRecord extends ConcurrentHashMap<OrderType, List<Order>>{
    public OrderRecord()
    {
        this.putIfAbsent(OrderType.ask, new ArrayList<>());
        this.putIfAbsent(OrderType.bid, new ArrayList<>());
    }
    public synchronized void AddOrder(Order newOrder)
    {
        int index = 0;
        List<Order> orderList = this.get(newOrder.type);

        for(; index < orderList.size(); index++)
        {
            if(compareOrder(newOrder, orderList.get(index))) break;
        }
        orderList.add(index, newOrder);
    }

    private boolean compareOrder(Order newOrder, Order listOrder)
    {
        if(newOrder.type == OrderType.ask) return newOrder.price < listOrder.price;
        else return newOrder.price > listOrder.price;
    }

    public synchronized int RemoveOrderById(int orderId, String username)
    {
        for(List<Order> orderList : this.values())
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
        }
        return 101;
    }

    public synchronized void RemoveAll(List<Order> ordersToRemove, OrderType type) {
        this.get(type).removeAll(ordersToRemove);
    }
}
