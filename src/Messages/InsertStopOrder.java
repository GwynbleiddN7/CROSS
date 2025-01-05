package Messages;

import Utility.OrderType;

public class InsertStopOrder extends MessageType{
    public OrderType type;
    public int size;
    public int price;
    public InsertStopOrder(OrderType type, int size, int price)
    {
        this.type = type;
        this.size = size;
        this.price = price;
    }
    @Override
    public String getOperation()
    {
        return "insertStopOrder";
    }
}
