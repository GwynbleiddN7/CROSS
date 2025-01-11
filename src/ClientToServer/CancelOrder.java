package ClientToServer;

import Utility.Operation;

//Messaggio Client->Server per la cancellazione di un ordine
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
