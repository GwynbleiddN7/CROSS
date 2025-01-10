package ServerToClient;

import Orders.Trade;

import java.util.List;

public class Notification {
    public final String notification = "closedTrades";
    public List<Trade> trades;
    public Notification(List<Trade> trades)
    {
        this.trades = trades;
    }
}
