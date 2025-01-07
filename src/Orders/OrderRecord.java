package Orders;

import Utility.OrderType;
import java.util.ArrayList;

public class OrderRecord {
    ArrayList<Order> ask;
    ArrayList<Order> bid;

    public OrderRecord()
    {
        ask = new ArrayList<>();
        bid = new ArrayList<>();
    }

    public void AddOrder(Order newOrder)
    {
        int index = 0;
        if(newOrder.type == OrderType.ask){
            for(Order order : ask)
            {
                if(order.price == newOrder.price) {
                    order.size += newOrder.size;
                    return;
                }
                else if(order.price < newOrder.price) {
                    index = ask.indexOf(order);
                    break;
                }
            }
            ask.add(index, newOrder);
        }
        else{
            for(Order order : bid)
            {
                if(order.price == newOrder.price) {
                    order.size += newOrder.size;
                    return;
                }
                else if(order.price > newOrder.price) {
                    index = bid.indexOf(order);
                    break;
                }
            }
            bid.add(index, newOrder);
        }
    }

    public void RemoveOrder(Order oldOrder)
    {
        if(oldOrder.type == OrderType.ask) ask.remove(oldOrder);
        else bid.remove(oldOrder);
    }
}
