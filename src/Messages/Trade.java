package Messages;

import Utility.OrderAction;
import Utility.OrderType;

public class Trade {
    public int orderId;
    public OrderType type;
    public OrderAction orderType;
    public int size;
    public int price;
    public long timestamp;
    public Trade(int orderId, OrderType type, OrderAction orderType, int size, int price, long timestamp)
    {
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
    }
}