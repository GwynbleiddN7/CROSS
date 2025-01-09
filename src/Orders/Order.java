package Orders;

import Utility.OrderType;

import java.util.Date;

public class Order {
    public String username;
    public int orderId;
    public OrderType type;
    public int size;
    public int price;
    public long timestamp;
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
