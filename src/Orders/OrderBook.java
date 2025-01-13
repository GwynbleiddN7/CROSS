package Orders;

import Server.NotificationHandler;
import Utility.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.util.*;

//Classe di gestione dell'orderBook, serializzata nel file orderBook.json
public class OrderBook {
    private OrderRecord stop; //Campo serializzato degli stop order
    private OrderRecord limit; //Campo serializzato dei limit order
    private static final String logsFile = "Data/ordersLog.json";
    private static final String orderBookFile = "Data/orderBook.json";
    private transient int maxId = -1; //Prossimo id da dare a un ordine, inizializzato a -1
    private transient List<Trade> logs; //Lista dei log delle transazioni usata per tenere traccia degli orderId e per loggare le transazioni

    public void LoadData()
    {
        LoadBook(); //Carica l'orderBook
        LoadLogs(); //Carica i log delle transazioni

        //Trova il prossimo id da dare a un nuovo ordine, trovando il massimo tra:
        for(List<Order> orders : limit.values()) //I limit order nell'orderBook
            orders.stream().max(Comparator.comparingInt(order -> order.orderId)).ifPresent(value -> maxId = Math.max(value.orderId, maxId));
        for(List<Order> orders : stop.values()) //I stop order nell'orderBook
            orders.stream().max(Comparator.comparingInt(order -> order.orderId)).ifPresent(value -> maxId = Math.max(value.orderId, maxId));
        //E tutti gli ordini evasi e loggati nel file
        logs.stream().max(Comparator.comparingInt(order -> order.orderId)).ifPresent(value -> maxId = Math.max(value.orderId, maxId));
        maxId++;
    }

    //Funzione per caricare i dati presente nell'orderBook, oppure per inizializzare i campi in caso il file non esista o abbia una sintassi errata
    private void LoadBook()
    {
        File orderBookFile = new File(OrderBook.orderBookFile);
        try{
            Gson gson = new Gson();
            OrderBook savedBook = gson.fromJson(new FileReader(orderBookFile), OrderBook.class);
            stop = savedBook.stop;
            limit = savedBook.limit;

        } catch (Exception e) {
            stop = OrderRecord.CreateEmptyOrderRecord();
            limit = OrderRecord.CreateEmptyOrderRecord();
        }
    }

    //Funzione per caricare i dati presente nell'orderLog, oppure per inizializzare la lista di transazioni in caso il file non esista o abbia una sintassi errata
    private void LoadLogs()
    {
        File logsFile = new File(OrderBook.logsFile);
        try{
            Gson gson = new Gson();
            TypeToken<List<Trade>> type = new TypeToken<>(){};
            List<Trade> logs = gson.fromJson(new FileReader(logsFile), type);

            this.logs = Collections.synchronizedList(logs);

        } catch (Exception e) {
            this.logs = Collections.synchronizedList(new ArrayList<>());
        }
    }

    //Funzione sincronizzata chiamata da ogni ClientHandler per aggiungere un nuovo ordine
    public synchronized int InsertNewOrder(String username, OrderType type, OrderAction orderType, int price, int size)
    {
        Order newOrder = new Order(maxId, username, type, price, size); //Crea un nuovo ordine inizializzando i dati
        int result = newOrder.orderId;
        switch (orderType)
        {
            case limit -> result = executeLimitOrder(newOrder);
            case market -> result = executeMarketOrder(newOrder, false);
            case stop -> result = executeStopOrder(newOrder);
        }
        if(result != -1) maxId++;
        OrderBookModified(); //Notifica una modifica nell'orderBook
        return result;
    }

    //Funzione sincronizzata chiamata da ogni ClientHandler per cancellare un ordine
    public synchronized int CancelOrder(String username, int orderId)
    {
        int resultLimit = limit.RemoveOrderById(orderId, username);
        int resultStop = stop.RemoveOrderById(orderId, username);

        OrderBookModified(); //Notifica una modifica nell'orderBook
        return resultLimit != 101 ? resultLimit : resultStop; //Sceglie il giusto codice da ritornare
    }

    //Funzione per eseguire i controlli dopo una modifica all'orderBook
    private void OrderBookModified()
    {
        checkForUpdate(); //Controlla limit order e stop order
        FileCreator.WriteToFile(orderBookFile, this); //Aggiorna l'orderBook nel file
        FileCreator.WriteToFile(logsFile, logs); //Aggiorna gli ordersLog nel file
    }

    //Funzione per provare a eseguire in market order (eventualmente trasformato da uno stop order)
    private int executeMarketOrder(Order order, boolean fromStopOrder)
    {
        try{
            calculateMarketOrder(order, fromStopOrder);
            return order.orderId;
        }
        catch (OrderFailedException e)
        {
            return -1; //è stato impossibile eseguire l'ordine
        }
    }

    //Funzione per gestire l'azione di un market order
    private void calculateMarketOrder(Order order, boolean fromStopOrder) throws OrderFailedException
    {
        ArrayList<Trade> logs = new ArrayList<>(); //Utenti da notificare in caso di successo
        List<Order> ordersToInteract = new ArrayList<>(); //Ordini che il market order coinvolge
        List<Order> ordersToRemove = new ArrayList<>(); //Ordini che il market order completa
        OrderType ordersType = order.type == OrderType.ask ? OrderType.bid : OrderType.ask; //Tipo di ordine tra cui il market order va a cercare

        List<Order> availableOrders = limit.get(ordersType);
        //Controlla se l'ordine andrà a buon fine prima di modificare
        int neededSize = order.size;
        for(Order availableOrder : availableOrders) { //Gli ordini sono già ordinati nel modo corretto, quindi scorro la lista partendo dal primo elemento
            if(order.username.equals(availableOrder.username)) continue; //Ignoro gli ordini dello stesso utente
            neededSize -= availableOrder.size;
            ordersToInteract.add(availableOrder); //Aggiungo l'ordine a quelli inclusi nella transazione
            if(neededSize >= 0) ordersToRemove.add(availableOrder); //Se la nuova differenza è positiva o 0, il market order ha esaurito l'ordine con cui ha interagito
            if(neededSize <= 0) break; //Se la nuova differenza è negativa o 0, il market order è esaurito e interrompo il ciclo di ordini con cui interagisce
        }
        if(neededSize > 0) throw new OrderFailedException(); //Se non ci sono abbastanza ordini per completare il market order interrompo

        //Eseguo l'operazione, che sicuramente andrà a buon fine perché il controllo è già stato fatto
        for(Order interactionOrder : ordersToInteract)
        {
            int size = Math.min(interactionOrder.size, order.size); //Quantità dell'ordine nel matching attuale
            double price = ((double) size / 1000.f) * ((double) interactionOrder.price / 1000.f); //Prezzo a cui viene eseguito il market order è quello dell'ordine nell'orderBook (prezzo effettivo, divido per 1000)

            //Aggiungo ai log le nuove transazioni (divido per 1000 per inviare i dati effettivi)
            logs.add(new Trade(interactionOrder.username, interactionOrder.orderId, interactionOrder.type, OrderAction.limit, (double) size / 1000.f, price));
            logs.add(new Trade(order.username, order.orderId, order.type, fromStopOrder ? OrderAction.stop : OrderAction.market, (double) size / 1000.f, price));

            //Aggiorno gli ordini
            interactionOrder.size -= size;
            order.size -= size;
        }
        limit.RemoveAll(ordersToRemove, ordersType); //Rimuovo gli ordini esauriti

        //Invio le notifiche ai Client e aggiorno il log delle transazioni
        NotificationHandler.Send(logs);
        AddLogs(logs);
    }

    //Funzione che esegue uno stop order
    private int executeStopOrder(Order newOrder) {
        if(checkStopTrigger(newOrder)) //Se lo stop order può essere eseguito subito lo trasformo in market order
            return executeMarketOrder(newOrder, true);
        else{
            stop.AddOrder(newOrder); //Altrimenti aggiungo l'ordine all'orderBook
            return newOrder.orderId;
        }
    }

    //Funzione che controlla il trigger degli stop order presenti nell'orderBook
    private void checkStopOrder()
    {
        List<Order> ordersToRemove = new ArrayList<>();
        for(List<Order> orders : stop.values())
        {
            for(Order stopOrder : orders)
            {
                if(checkStopTrigger(stopOrder))
                {
                    executeMarketOrder(stopOrder, true); //Se il trigger è valido attivo lo stop order
                    ordersToRemove.add(stopOrder); //E lo flaggo come da rimuovere dall'orderBook
                }
            }
            orders.removeAll(ordersToRemove); //Rimuovo dall'orderBook tutti gli ordini flaggati
        }
    }

    //Funzione che controlla il trigger di uno stop order
    private boolean checkStopTrigger(Order order)
    {
        try{
            if(order.type == OrderType.ask)
            {
                return getMarketPrice(OrderType.bid, order.username) <= order.price; //Se il prezzo di mercato scende sotto lo stop price di vendita
            }
            else
            {
                return getMarketPrice(OrderType.ask, order.username) >= order.price; //Se il prezzo di mercato sale sopra lo stop price di acquisto
            }
        }catch (EmptyMarketException _)
        {
            return false; //Se il mercato è vuoto lascio l'ordine nell'orderBook
        }
    }

    //Funzione che ritorna il prezzo di mercato utilizzato dagli stop order per controllare il proprio trigger
    private int getMarketPrice(OrderType type, String username) throws EmptyMarketException
    {
        try{
            //Ritorno il prezzo del primo ordine (lista già ordinata correttamente) tra i limit order che non hanno come username lo stesso dell'ordine da controllare
            return limit.get(type).stream().filter(order -> !order.username.equals(username)).toList().getFirst().price;
        }
        catch (NoSuchElementException _) {
            throw new EmptyMarketException();
        }
    }

    //Funzione che esegue un limit order
    private int executeLimitOrder(Order newOrder) {
        limit.AddOrder(newOrder); //I limit order vengono aggiunti sempre all'orderBook, e successivamente viene controllato se possono essere eseguiti anche parzialmente
        return newOrder.orderId;
    }

    //Algoritmo di matching del limit orders
    private void matchLimitOrders()
    {
        HashMap<OrderType, ArrayList<Order>> ordersToRemove = new HashMap<>(); //Ordini da rimuove dall'orderBook, divisi per tipo
        //Inizializzo la HashMap
        ordersToRemove.put(OrderType.ask, new ArrayList<>());
        ordersToRemove.put(OrderType.bid, new ArrayList<>());
        ArrayList<Trade> logs = new ArrayList<>(); //Log delle transazioni effettuate

        //Cicli annidati che eseguono il matching (le liste sono già ordinate secondo la price priority, quindi le scandisco dal primo elemento)
        for(Order bidOrder : limit.get(OrderType.bid)) //Eseguo il match di ogni bid order
        {
            for(Order askOrder : limit.get(OrderType.ask)) //Con ogni ask order
            {
                if(askOrder.username.equals(bidOrder.username)) continue; //Ignoro gli ordini dello stesso utente

                int size = 0;
                int price = 0;
                if(bidOrder.price >= askOrder.price) //Se le clausole di prezzo degli ordini sono rispettate eseguo la transazione
                {
                    size = Math.min(askOrder.size, bidOrder.size); //Imposto la massima grandezza dell'ordine possibile
                    //Aggiorno gli ordini coinvolti
                    askOrder.size -= size;
                    bidOrder.size -= size;
                    //Assegno il prezzo corretto secondo la time priority
                    price = askOrder.timestamp <= bidOrder.timestamp ? askOrder.price : bidOrder.price;
                    //Flaggo come da rimuovere gli ordini che sono terminati
                    if(askOrder.size <= 0) ordersToRemove.get(OrderType.ask).add(askOrder);
                    if(bidOrder.size <= 0) ordersToRemove.get(OrderType.bid).add(bidOrder);
                }
                //Se la transazione è avvenuta aggiungo i log da notificare ai client e da salvare nell'ordersLog
                if(size > 0)
                {
                    logs.add(createLimitLog(askOrder, size, price));
                    logs.add(createLimitLog(bidOrder, size, price));
                }
                if(bidOrder.size <= 0) break; //Se il bid order è concluso passo al successivo
            }
            limit.RemoveAll(ordersToRemove.get(OrderType.ask), OrderType.ask); //Rimuovo tutti gli ask order conclusi prima di ricominciare a scandire la lista
            ordersToRemove.get(OrderType.ask).clear(); //Rimuovo gli ordini rimossi dalla lista di quelli da rimuovere
        }
        limit.RemoveAll(ordersToRemove.get(OrderType.bid), OrderType.bid); //Rimuovo tutti i bid order conclusi

        //Invio le notifiche ai Client e aggiorno il log delle transazioni
        NotificationHandler.Send(logs);
        AddLogs(logs);
    }

    //Funzione per creare i log per i limit order conclusi o parzialmente eseguiti (divido per 1000 per creare i log effettivi)
    private Trade createLimitLog(Order order, int size, int price)
    {
        return new Trade(order.username, order.orderId, order.type, OrderAction.limit, (double) size / 1000.f, ((double) size / 1000.f) * ((double) price / 1000.f));
    }

    //Funzione che controlla se ci sono stop order da attivare, successivamente se ci sono limit order da matchare e infine ricontrolla eventuali stop order da attivare
    private void checkForUpdate()
    {
        checkStopOrder();
        matchLimitOrders();
        checkStopOrder();
    }

    //Funzione che aggiunge nuovi trade in testa alla lista delle transazioni
    private void AddLogs(ArrayList<Trade> logs) {
        this.logs.addAll(0, logs);
    }
}