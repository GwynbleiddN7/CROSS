package Orders;

import Utility.OrderType;

public class Order {
    public String username;
    public int orderId;
    public OrderType type;
    public int size;
    public int price;
    public Order(String username, OrderType type, int price, int size)
    {
        this.username = username;
        this.type = type;
        this.price = price;
        this.size = size;
    }
}
