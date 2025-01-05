package Messages;
import Utility.OrderType;

public class InsertLimitOrder extends MessageType{
    public OrderType type;
    public int size;
    public int price;
    public InsertLimitOrder(OrderType type, int size, int price)
    {
        this.type = type;
        this.size = size;
        this.price = price;
    }
    @Override
    public String getOperation()
    {
        return "insertLimitOrder";
    }
}
