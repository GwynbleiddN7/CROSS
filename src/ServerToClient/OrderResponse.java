package ServerToClient;

public class OrderResponse extends ResponseMessage {
    public final int orderId;
    public OrderResponse(int orderId)
    {
        this.orderId = orderId;
    }
}
