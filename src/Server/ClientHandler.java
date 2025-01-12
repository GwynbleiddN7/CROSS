package Server;

import ClientToServer.*;
import Orders.OrderBook;
import ServerToClient.*;
import Utility.Operation;
import Utility.OrderAction;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

public class ClientHandler implements Runnable{
    private final Socket client;
    private final InetSocketAddress udpSocket;
    private final OrderBook orderBook;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final String ordersHistoryPath = "Data/storicoOrdini.json";
    private Credentials currentCredentials = null;

    public ClientHandler(Socket client, OrderBook orderBook) throws IOException {
        //Inizializzo le variabili usate dal thread
        this.client = client;
        this.orderBook = orderBook;

        //Preparo gli stream per la lettura e per la scrittura
        this.out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(client.getInputStream()));

        int udpPort = in.readInt(); //Leggo la porta che il client ha scelto per ricevere i messaggi UDP (primo messaggio inviato nella socket TCP)
        this.udpSocket = new InetSocketAddress(this.client.getInetAddress(), udpPort); //Creo la socket UDP su cui verranno inviate le notifiche asincrone al client

        this.client.setSoTimeout(1000 * ServerMain.timeout); //Imposto un timeout in secondi (default 5 minuti) per eseguire il logout per inattività
    }

    //Funzione asincrona che si occupa di gestire la connessione con un client
    public void run() {

        try
        {
            while(true)
            {
                try
                {
                    //Leggo la richiesta del client
                    int size = in.readInt(); //Leggo la lunghezza del messaggio che sta per arrivare
                    byte[] buff = new byte[size]; //Alloco lo spazio necessario
                    int len = in.read(buff, 0, size); //Leggo il messaggio dalla stream

                    String answer = new String(buff, 0, len); //Converto il messaggio in stringa
                    String response = parseMessage(answer); //Eseguo l'operazione richiesta dal messaggio
                    if(response.equals("exit")) break; //Se era richiesta un disconnessione, interrompo il ciclo di lettura

                    //Invio il risultato dell'operazione al client
                    out.writeInt(response.length()); //Comunico al client quanto è grande il messaggio che sta per arrivare
                    out.writeBytes(response); //Invio il messaggio effettivo
                    out.flush();
                }
                catch (SocketTimeoutException _)
                {
                    currentCredentials = null; //Se è avvenuto un timeout, significa che il client è inattivo e faccio logout ma non chiudo la connessione
                }
            }
            //Interrompo la connessione e chiudo le stream
            out.close();
            in.close();
            client.close();
        }
        catch (IOException _)
        {
            System.out.println("Errore nella comunicazione con il client");
        }
        finally {
            currentCredentials = null; //Eseguo il logout per sicurezza
            ServerMain.RemoveClient(this); //Rimuovo il client da quelli connessi al server e si interrompe il thread
        }
    }

    //Funzione che si occupa di eseguire l'operazione richiesta dal client
    private String parseMessage(String receivedMessage)
    {
        ResponseMessage responseMessage; //Messaggio di tipo generico
        Gson gson = new Gson();
        try {
            //Prima di de-serializzare completamente la stringa leggo l'operazione, e poi de-serializzo il cambio 'values' nell'oggetto dell'operazione corretta
            JsonElement element = JsonParser.parseString(receivedMessage);
            String operation = element.getAsJsonObject().get("operation").getAsString(); //Leggo il tipo dell'operazione
            JsonElement values = element.getAsJsonObject().get("values"); //Leggo i valori, la cui struttura dipende dal tipo dell'operazione

            int response;
            String errorMessage;

            //Controllo quale operazione è stata richiesta (IllegalArgumentException se non è un'operazione supportata)
            switch (Operation.valueOf(operation))
            {
                case Operation.exit -> {
                    return "exit";
                }
                case Operation.register -> {
                    if (currentCredentials != null) response = 103; //Se sono già loggato non posso registrarmi
                    else {
                        Registration registration = gson.fromJson(values, Registration.class); //De-serializzo i valori nell'oggetto corretto
                        response = LoginHandler.RegisterUser(registration.username, registration.password); //Eseguo la registrazione
                    }

                    //In base al codice ritornato dall'operazione imposto il testo del messaggio
                    errorMessage = switch (response) {
                        case 100 -> "OK";
                        case 101 -> "invalid password";
                        case 102 -> "username not available";
                        case 103 -> "user currently logged in";
                        default -> "unknown error";
                    };
                    responseMessage = new StandardResponse(response, errorMessage); //Creo l'oggetto della risposta
                }
                case Operation.updateCredentials -> {
                    if(currentCredentials != null) response = 104; //Se sono già loggato non posso cambiare password
                    else{
                        UpdateCredentials updateCredentials = gson.fromJson(values, UpdateCredentials.class); //De-serializzo i valori nell'oggetto corretto
                        response = LoginHandler.UpdateCredentials(updateCredentials.username, updateCredentials.old_password, updateCredentials.new_password); //Eseguo il cambio password
                    }

                    //In base al codice ritornato dall'operazione imposto il testo del messaggio
                    errorMessage = switch (response) {
                        case 100 -> "OK";
                        case 101 -> "invalid new password";
                        case 102 -> "username/old password mismatch or non-existent username";
                        case 103 -> "new password equal to old one";
                        case 104 -> "user currently logged in";
                        default -> "unknown error";
                    };
                    responseMessage = new StandardResponse(response, errorMessage); //Creo l'oggetto della risposta
                }
                case Operation.login -> {
                    if(currentCredentials != null) response = 102; //Se sono già loggato non posso fare login
                    else{
                        Login login = gson.fromJson(values, Login.class); //De-serializzo i valori nell'oggetto corretto
                        response = LoginHandler.LoginUser(login.username, login.password); //Eseguo il login
                        if(response == 100) currentCredentials = new Credentials(login.username, login.password); //Se è avvenuto con successo imposto le credenziali correnti
                    }

                    //In base al codice ritornato dall'operazione imposto il testo del messaggio
                    errorMessage = switch (response) {
                        case 100 -> "OK";
                        case 101 -> "username/password mismatch or non-existent username";
                        case 102 -> "user currently logged in";
                        case 103 -> "error updating credentials file";
                        default -> "unknown error";
                    };
                    responseMessage = new StandardResponse(response, errorMessage); //Creo l'oggetto della risposta
                }
                case Operation.logout -> {
                    if(currentCredentials == null) response = 101; //Se non sono loggato non posso fare logout
                    else  response = 100; //Altrimenti l'operazione avrà successo
                    currentCredentials = null; //Rimuovo in qualunque caso le credenziali correnti

                    //In base al codice ritornato dall'operazione imposto il testo del messaggio
                    errorMessage = switch (response) {
                        case 100 -> "OK";
                        case 101 -> "user not logged in";
                        default -> "unknown error";
                    };
                    responseMessage = new StandardResponse(response, errorMessage); //Creo l'oggetto della risposta
                }
                case Operation.insertLimitOrder -> {
                    if(currentCredentials == null) {  //Se non sono loggato non posso inserire un ordine
                        responseMessage = new OrderResponse(-1); //Imposto il messaggio di ritorno con orderId = -1 ed esco
                        break;
                    }
                    InsertLimitOrder insertLimitOrder = gson.fromJson(values, InsertLimitOrder.class); //De-serializzo i valori nell'oggetto corretto
                    //Inserisco l'ordine nell'orderBook
                    int limitOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertLimitOrder.type, OrderAction.limit, insertLimitOrder.price, insertLimitOrder.size);
                    responseMessage = new OrderResponse(limitOrderId); //Creo l'oggetto della risposta con orderId quello ritornato dal risultato dell'aggiunta all'orderBook
                }
                case Operation.insertMarketOrder -> {
                    if(currentCredentials == null) { //Se non sono loggato non posso inserire un ordine
                        responseMessage = new OrderResponse(-1); //Imposto il messaggio di ritorno con orderId = -1 ed esco
                        break;
                    }
                    InsertMarketOrder insertMarketOrder = gson.fromJson(values, InsertMarketOrder.class); //De-serializzo i valori nell'oggetto corretto
                    //Inserisco l'ordine nell'orderBook
                    int marketOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertMarketOrder.type, OrderAction.market, 0, insertMarketOrder.size);
                    responseMessage = new OrderResponse(marketOrderId); //Creo l'oggetto della risposta con orderId quello ritornato dal risultato dell'aggiunta all'orderBook
                }
                case Operation.insertStopOrder -> {
                    if(currentCredentials == null) { //Se non sono loggato non posso inserire un ordine
                        responseMessage = new OrderResponse(-1); //Imposto il messaggio di ritorno con orderId = -1 ed esco
                        break;
                    }
                    InsertStopOrder insertStopOrder = gson.fromJson(values, InsertStopOrder.class); //De-serializzo i valori nell'oggetto corretto
                    //Inserisco l'ordine nell'orderBook
                    int stopOrderId = orderBook.InsertNewOrder(currentCredentials.username, insertStopOrder.type, OrderAction.stop, insertStopOrder.price, insertStopOrder.size);
                    responseMessage = new OrderResponse(stopOrderId); //Creo l'oggetto della risposta con orderId quello ritornato dal risultato dell'aggiunta all'orderBook
                }
                case Operation.cancelOrder -> {
                    if(currentCredentials == null) { //Se non sono loggato non posso cancellare un ordine
                        responseMessage = new OrderResponse(-1); //Imposto il messaggio di ritorno con orderId = -1 ed esco
                        break;
                    }
                    CancelOrder cancelOrder = gson.fromJson(values, CancelOrder.class); //De-serializzo i valori nell'oggetto corretto
                    int cancelledResult = orderBook.CancelOrder(currentCredentials.username, cancelOrder.orderId); //Cancello l'ordine dall'orderBook

                    //In base al codice ritornato dall'operazione imposto il testo del messaggio
                    errorMessage = switch (cancelledResult) {
                        case 100 -> "OK";
                        case 101 -> "order does not exist or has been finalized";
                        case 102 -> "order belongs to a different user";
                        default -> "unknown error";
                    };
                    responseMessage = new StandardResponse(Math.min(cancelledResult, 101), errorMessage); //Creo l'oggetto della risposta
                }
                case Operation.getPriceHistory -> {
                    if(currentCredentials == null) { //Se non sono loggato non posso richiedere la priceHistory
                        responseMessage = new StandardResponse(101, "user not logged in"); //Creo l'oggetto della risposta con errore ed esco
                        break;
                    }
                    GetPriceHistory getPriceHistory = gson.fromJson(values, GetPriceHistory.class); //De-serializzo i valori nell'oggetto corretto
                    boolean scanResult = handlePurchaseHistory(getPriceHistory); //Eseguo lo scan della purchaseHistory

                    //Se lo scan ha avuto successo ritorno un messaggio positivo, altrimenti ritorno un messaggio negativo (sempre come ultimo messaggio, dopo aver già inviato al client la history completa)
                    if(scanResult) responseMessage = new StandardResponse(100, "history scan complete");  //Creo l'oggetto della risposta con successo
                    else responseMessage = new StandardResponse(102, "history scan failed");  //Creo l'oggetto della risposta con errore
                }
                default -> responseMessage = new StandardResponse(404, "command not found");  //Creo l'oggetto della risposta in caso non trovo un'operazione valida
            }
            return gson.toJson(responseMessage); //Serializzo l'oggetto della risposta e lo ritorno all'handler
        }
        catch (IllegalArgumentException _)
        {
            return gson.toJson(new StandardResponse(404, "command not found")); //Creo l'oggetto serializzato della risposta se c'è stato un errore
        }
        catch (JsonSyntaxException _)
        {
            return gson.toJson(new StandardResponse(400, "bad request")); //Creo l'oggetto serializzato della risposta se c'è stato un errore nella de-serializzazione
        }
    }

    //Funzione che si occupa di inviare al client la priceHistory del mese desiderato
    private boolean handlePurchaseHistory(GetPriceHistory priceHistory) {
        try {
            //Imposto il formatter nello stesso modo in cui la stringa arriva dal client
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("MMyyyy")
                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1) //Imposto il minimo giorno del mese selezionato
                    .toFormatter();
            LocalDate date = LocalDate.parse(priceHistory.month, formatter); //Estraggo la data dal testo ricevuto
            LocalDateTime gmtDate = date.atStartOfDay(); //Imposto il minimo timestamp relativo al mese selezionato

            //Imposto i parametri per prendere il timestamp massimo e minimo del mese da leggere
            long endingBound = Date.from(gmtDate.plusMonths(1).toInstant(ZoneOffset.UTC)).getTime(); //Imposto il timestamp massimo (quello dell'inizio del mese successivo)
            long lowerBound = Date.from(gmtDate.toInstant(ZoneOffset.UTC)).getTime(); //Imposto il timestamp minimo iniziale (quello dell'inizio del mese)
            long upperBound = lowerBound; //Imposto il timestamp massimo per ogni giorno (verrà modificato successivamente, per ora inizializzato come il lowerBound)

            //Inizializzazione valori di default
            long defaultTime = new Date().getTime();
            int day = 0;
            ArrayList<PriceHistory> prices = new ArrayList<>(); //Lista delle priceHistory lette dal file per ogni giorno
            List<Thread> threads = new ArrayList<>(); //Lista dei thread che si occuperanno di mandare al client blocchi di priceHistory

            //Leggo come stream il file dello storico ordini (data la grandezza del file non lo salvo tutto insieme in memoria)
            JsonReader reader = new JsonReader(new FileReader(ordersHistoryPath));
            reader.beginObject();
            while (reader.hasNext()){
                String name = reader.nextName();
                if ("trades".equals(name)){ //Lista che contiene tutte le informazioni sui trade
                    reader.beginArray();

                    PriceHistory currentPrice = null; //Inizializzo il currentPrice, cioè le informazioni sui prezzi relativi a una giornata

                    boolean done = false; //Flag di fine iterazione
                    while (reader.hasNext()){
                        if(done) { //Se ho finito con il mese, completo la lettura e termino senza eseguire calcoli
                            reader.skipValue();
                            continue;
                        }
                        reader.beginObject();
                        //Inizializzo le variabili temporanee con valori di default
                        int price = 0;
                        long time = defaultTime;

                        while (reader.hasNext()){
                            String obj = reader.nextName();
                            if ("price".equals(obj)) price = reader.nextInt(); //Salvo temporaneamente il prezzo letto
                            else if("timestamp".equals(obj)) time = reader.nextLong() * 1000; //Salvo temporaneamente il timestamp letto (*1000 perché il timestamp letto da Java ha ulteriori 3 cifre finali poste a 0 rispetto a quelle del file)
                            else reader.skipValue(); //Gli altri campi non mi interessano quindi evito di salvarli e occupare memoria inutilmente
                        }

                        if(time >= lowerBound) //Se il timestamp letto risulta maggiore del minimo (cioè il giorno in cui è avvenuto il trade è potenzialmente all'interno del mese che sto cercando)
                        {
                            if(time < endingBound) //Se il timestamp letto risulta minore del massimo (cioè il giorno in cui è avvenuto il trade è sicuramente all'interno del mese che sto cercando)
                            {
                                if(time >= upperBound) //Se ho superato l'upperBound è cambiato giorno, quindi aggiorno di conseguenza le variabili (oppure se è la prima volta che arrivo qui nel codice)
                                {
                                    if(prices.size() == 8) //Decido di mandare al client al massimo le informazioni di 8 giorni alla volta, per limitare la grandezza del pacchetto TCP e ottimizzare i tempi
                                    {
                                        sendPriceHistory(prices, threads); //Invio, con un altro thread, le informazioni al client mentre continuo a processare le successive
                                        prices.clear(); //Le informazioni inviate posso eliminarle
                                    }

                                    day++; //Incremento il numero del giorno
                                    lowerBound = upperBound; //Resetto il lowerBound
                                    gmtDate = gmtDate.plusDays(1); //Aggiorno la data al giorno successivo
                                    upperBound = Date.from(gmtDate.toInstant(ZoneOffset.UTC)).getTime(); //Aggiorno l'upperBound con il timestamp dell'inizio del giorno successivo

                                    currentPrice = new PriceHistory(); //Resetto le informazioni del giorno corrente
                                    currentPrice.day = day; //Imposto il giorno corrente
                                    prices.add(currentPrice); //Aggiungo le nuove informazioni alla lista da inviare successivamente al client
                                }
                                if(currentPrice != null)
                                {
                                    if(currentPrice.openingPrice == -1) currentPrice.openingPrice = price; //Imposto l'openingPrice (inizializzato a -1 per garantire che venga impostato solo la prima volta)
                                    currentPrice.closingPrice = price; //Aggiorno ogni volta il closingPrice in modo tale che all'ultima iterazione venga impostato correttamente l'ultimo prezzo della giornata
                                    currentPrice.maxPrice = Math.max(price, currentPrice.maxPrice); //Aggiorno il prezzo massimo
                                    currentPrice.minPrice = Math.min(price, currentPrice.minPrice); //Aggiorno il prezzo minimo
                                }
                            }
                        }
                        reader.endObject();
                        if(time >= endingBound) { //Se il timestamp è maggiore o uguale a quello dell'inizio del nuovo mese devo fermarmi
                            sendPriceHistory(prices, threads); //Invio al client eventuali ultime informazioni rimaste in coda (Per gli ultimi giorni o in caso l'ultima iterazione non fosse arrivata a 8 elementi)
                            done = true; //Imposto la flag di fine
                        }
                    }
                    reader.endArray();
                } else reader.skipValue();
            }
            reader.endObject();
            reader.close();

            for(Thread t : threads) t.join(); //Attendo che tutti i thread abbiano finito di inviare al client i dati
        } catch (IOException | InterruptedException _) { return false; } //In caso di errori nell'esecuzione ritorno false
        return true; //In caso di successo ritorno true
    }

    //Funzione che delega a un thread l'invio al client di un sottoinsieme (di massimo 8 giorni) di PriceHistory relative ai giorni di un mese
    private void sendPriceHistory(ArrayList<PriceHistory> prices, List<Thread> threads)
    {
        if(prices.isEmpty()) return; //Se non ci sono dati per il sottoinsieme di giorni corrente non invio niente
        final PriceResponse priceResponse = new PriceResponse(new ArrayList<>(prices)); //Creo l'oggetto della classe PriceResponse con la sottolista attuale
        Thread thread = new Thread(() -> {
            Gson gson = new Gson();
            String msg = gson.toJson(priceResponse); //Serializzo l'oggetto
            synchronized (out)
            {
                //Invio il messaggio tramite lo stream di output
                try {
                    out.writeInt(msg.length()); //Comunico al client quanto è grande il messaggio che sta per arrivare
                    out.writeBytes(msg); //Invio il messaggio al client
                    out.flush();
                } catch (IOException e) {
                    Thread.currentThread().interrupt(); //Interrompo il thread se riscontro un errore
                }
            }
        });
        thread.start(); //Avvio il Runnable nel thread
        threads.add(thread); //Aggiungo il thread alla lista di thread da aspettare prima di comunicare il termine dell'operazione al client
    }

    //Funzione che ritorna il socket address UDP del client connesso con l'username specificato
    public InetSocketAddress GetAddressIfLogged(String username)
    {
        if(currentCredentials != null)
        {
            if(currentCredentials.username.equals(username)) return udpSocket; //Se lo trovo lo ritorno
        }
        return null; //Altrimenti ritorno null (utilizzato in best-effort)
    }
}
