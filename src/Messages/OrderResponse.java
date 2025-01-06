package Messages;

public class OrderResponse extends ResponseMessage{
    public int orderId;
    public OrderResponse(int orderId)
    {
        this.orderId = orderId;
    }
}
