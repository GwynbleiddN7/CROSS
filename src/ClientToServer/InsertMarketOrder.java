package ClientToServer;

import Utility.Operation;
import Utility.OrderType;

//Messaggio Client->Server per l'inserimento di un market order
public class InsertMarketOrder extends MessageType{
    public final OrderType type;
    public final int size;
    public InsertMarketOrder(OrderType type, int size)
    {
        this.type = type;
        this.size = size;
    }
    @Override
    public Operation getOperation()
    {
        return Operation.insertMarketOrder;
    }
}
