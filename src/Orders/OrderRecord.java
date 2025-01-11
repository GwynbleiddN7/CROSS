package Orders;

import Utility.OrderType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//Classe che rappresenta lista di ordini di tipo ask e liste di ordini di tipo bid, utilizzata nell'orderBook per suddividere ordini limit e stop
public class OrderRecord extends ConcurrentHashMap<OrderType, List<Order>>{
    public OrderRecord()
    {
        this.putIfAbsent(OrderType.ask, new ArrayList<>());
        this.putIfAbsent(OrderType.bid, new ArrayList<>());
    }

    //Funzione sincronizzata, utilizzata per aggiungere un nuovo ordine all'orderBook
    public synchronized void AddOrder(Order newOrder)
    {
        int index = 0;
        List<Order> orderList = this.get(newOrder.type);

        //Inserimento nella lista già ordinato, così da non dover fare sort successive e poter accedere al primo elemento della lista per ricavare l'ordine utile all'algoritmo di matching
        for(; index < orderList.size(); index++)
        {
            if(compareOrder(newOrder, orderList.get(index))) break; //Controlla l'ordinamento crescente o decrescente in base alla tipologia dell'ordine
        }
        orderList.add(index, newOrder);
    }

    //Ordinamento crescente per ask e decrescente per bid
    private boolean compareOrder(Order newOrder, Order listOrder)
    {
        if(newOrder.type == OrderType.ask) return newOrder.price < listOrder.price;
        else return newOrder.price > listOrder.price;
    }

    //Funzione sincronizzata, utilizzata per rimuovere un nuovo ordine all'orderBook tramite id
    public synchronized int RemoveOrderById(int orderId, String username)
    {
        for(List<Order> orderList : this.values()) //Cerca tra tutti gli ask e i bid
        {
            for(Order order : orderList)
            {
                if(order.orderId == orderId)
                {
                    if(username.equals(order.username)) //Controlla che l'utente sia il proprietario dell'ordine
                    {
                        orderList.remove(order);
                        return 100;
                    }
                    else return 102; //Codice di errore per mismatch utente
                }
            }
        }
        return 101; //Codice di errore per ordine non trovato
    }

    //Funzione sincronizzata, utilizzata per rimuovere una lista di ordini tramite il tipo
    public synchronized void RemoveAll(List<Order> ordersToRemove, OrderType type)
    {
        this.get(type).removeAll(ordersToRemove);
    }
}
