package ServerToClient;

//Risposta Server->Client con campo 'orderId', utilizzata come risposta alla creazione di un ordine
public class OrderResponse extends ResponseMessage {
    public final int orderId;
    public OrderResponse(int orderId)
    {
        this.orderId = orderId;
    }
}
