package ServerToClient;

import Orders.Trade;
import java.util.List;

//Risposta Server->Client UDP con campo 'trades', utilizzata per informare i Client del completamento di una o più transazioni
public class Notification {
    public final String notification = "closedTrades";
    public List<Trade> trades;
    public Notification(List<Trade> trades)
    {
        this.trades = trades;
    }
}
