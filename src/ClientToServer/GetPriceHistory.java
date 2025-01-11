package ClientToServer;

import Utility.Operation;

//Messaggio Client->Server per richiedere la priceHistory
public class GetPriceHistory extends MessageType{
    public final String month;
    public GetPriceHistory(String month)
    {
        this.month = month;
    }
    @Override
    public Operation getOperation()
    {
        return Operation.getPriceHistory;
    }
}
