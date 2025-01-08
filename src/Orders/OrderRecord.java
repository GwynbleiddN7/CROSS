package Orders;

import Utility.OrderType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OrderRecord extends ConcurrentHashMap<OrderType, List<Order>>{

    public OrderRecord()
    {
        super();
        this.put(OrderType.ask, Collections.synchronizedList(new ArrayList<>()));
        this.put(OrderType.bid, Collections.synchronizedList(new ArrayList<>()));
    }

    public OrderRecord(OrderRecord oldRecord)
    {
        super();
        this.put(OrderType.ask, Collections.synchronizedList(oldRecord.get(OrderType.ask)));
        this.put(OrderType.bid, Collections.synchronizedList(oldRecord.get(OrderType.bid)));
    }

    public void AddOrder(Order newOrder)
    {
        int index = 0;
        List<Order> orderList;
        if(newOrder.type == OrderType.ask){
            orderList = this.get(OrderType.ask);
            for(Order order : orderList)
            {
                if(order.price < newOrder.price) {
                    index = orderList.indexOf(order);
                    break;
                }
            }
        }
        else{
            orderList = this.get(OrderType.bid);
            for(Order order : orderList)
            {
                if(order.price > newOrder.price) {
                    index = orderList.indexOf(order);
                    break;
                }
            }
        }
        orderList.add(index, newOrder);
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

    public boolean RemoveOrder(Order oldOrder)
    {
        return this.get(oldOrder.type).remove(oldOrder);
    }

    public void RemoveAll(List<Order> ordersToRemove, OrderType type) {
        this.get(type).removeAll(ordersToRemove);
    }
}
