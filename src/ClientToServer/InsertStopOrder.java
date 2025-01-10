package ClientToServer;

import Utility.Operation;
import Utility.OrderType;

public class InsertStopOrder extends MessageType{
    public final OrderType type;
    public final int size;
    public final int price;
    public InsertStopOrder(OrderType type, int size, int price)
    {
        this.type = type;
        this.size = size;
        this.price = price;
    }
    @Override
    public Operation getOperation()
    {
        return Operation.insertStopOrder;
    }
}
