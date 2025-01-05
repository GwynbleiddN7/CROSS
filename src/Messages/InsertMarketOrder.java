package Messages;

import Utility.OrderType;

public class InsertMarketOrder extends MessageType{
    public OrderType type;
    public int size;
    public InsertMarketOrder(OrderType type, int size)
    {
        this.type = type;
        this.size = size;
    }
    @Override
    public String getOperation()
    {
        return "insertMarketOrder";
    }
}
