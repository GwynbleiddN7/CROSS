package ClientToServer;

import Utility.Operation;

public class CancelOrder extends MessageType{
    public final int orderId;
    public CancelOrder(int orderId)
    {
        this.orderId = orderId;
    }
    @Override
    public Operation getOperation()
    {
        return Operation.cancelOrder;
    }
}
