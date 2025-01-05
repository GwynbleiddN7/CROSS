package Messages;

import java.util.ArrayList;

public class Notification {
    public String notification = "closedTrades";
    public ArrayList<Trade> trades;
    public Notification(ArrayList<Trade> trades)
    {
        this.trades = trades;
    }
}
