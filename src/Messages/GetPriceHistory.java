package Messages;

public class GetPriceHistory extends MessageType{
    public String month;
    public GetPriceHistory(String month)
    {
        this.month = month;
    }
    @Override
    public String getOperation()
    {
        return "getPriceHistory";
    }
}
