package Messages;

public class CancelOrder extends MessageType{
    public int orderId;
    public CancelOrder(int orderId)
    {
        this.orderId = orderId;
    }
    @Override
    public String getOperation()
    {
        return "cancelOrder";
    }
}
