package Orders;

import Utility.OrderType;
import java.util.Date;

//Informazioni degli ordini salvati nell'OrderBook
public class Order {
    public final String username;
    public final int orderId;
    public final OrderType type;
    public final long timestamp;

    public int size;
    public int price;
    public Order(int orderId, String username, OrderType type, int price, int size)
    {
        this.username = username;
        this.type = type;
        this.price = price;
        this.size = size;

        this.orderId = orderId;
        this.timestamp = new Date().getTime();
    }
}
