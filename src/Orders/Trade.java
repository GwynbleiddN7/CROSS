package Orders;

import Utility.OrderAction;
import Utility.OrderType;
import java.util.Date;

//Informazioni degli ordini inviati come notifica ai Client e salvati nel file ordersLog.json
public class Trade {
    public final transient String username; //Utilizzato solo per raggruppare le notifiche, non viene salvato nel file di log
    public final int orderId;
    public final OrderType type;
    public final OrderAction orderType;
    public final int size;
    public final int price;
    public final long timestamp;
    public Trade(String username, int orderId, OrderType type, OrderAction orderType, int size, int price)
    {
        this.username = username;
        this.orderId = orderId;
        this.type = type;
        this.orderType = orderType;
        this.size = size;
        this.price = price;
        this.timestamp = new Date().getTime();
    }
}
