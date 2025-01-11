package Client;

import ClientToServer.*;
import ServerToClient.OrderResponse;
import ServerToClient.PriceHistory;
import ServerToClient.PriceResponse;
import ServerToClient.StandardResponse;
import Utility.Operation;
import Utility.OrderType;
import Utility.IncorrectParameterException;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.google.gson.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.TimeZone;

public class ClientMain {
    private static final String configFile = "client.properties";
    private static String host; //host su cui eseguire il binding della socket TCP
    private static int tcp_port; //porta su cui eseguire il binding della socket TCP
    private final static Gson gson = new Gson();
    private static final Scanner console = new Scanner(System.in); //Scanner dell'input console dell'utente

    public static void main(String[] args) {
        try {
            readConfig(); //Provo a leggere le configurazione dal file client.properties
        } catch (IOException e) {
            System.out.println("Errore nella lettura del file di configurazione");
            return;
        }

        try(Socket socket = new Socket(host, tcp_port)) //Creo la socket TCP
        {
            //Inizializzo gli stream di lettura e scrittura della socket
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            NotificationHandler notificationHandler = new NotificationHandler(); //Creo l'oggetto che si occupa di ricevere i datagram UDP asincroni
            out.writeInt(notificationHandler.GetPort()); //Comunico al server la porta su cui il client desidera ricevere i datagram UDP

            Thread notificationThread = new Thread(notificationHandler);
            notificationThread.start(); //Avvio il servizio di ricezione notifiche

            System.out.println("Inserisci un comando [format: cmd(param1, param2, ...)]");
            while(true)
            {
                System.out.print(">");
                String input = console.nextLine(); //Leggo l'input dell'utente

                MessageType msg = parseInput(input); //Converto l'input in un messaggio valido da mandare al server
                if(msg == null) continue; //Se il messaggio non è valido ignoro il resto

                Message<MessageType> message = new Message<>(msg); //Trasformo il messaggio nel formato da mandare al server con tutte le informazioni relative
                String stringMessage = gson.toJson(message); //Serializzo il messaggio
                out.writeInt(stringMessage.length()); //Comunico al server quanto è grande il messaggio che sta per arrivare
                out.writeBytes(stringMessage); //Invio il messaggio effettivo
                out.flush();

                if(msg.getOperation() == Operation.exit) break; //Operazione che termina la connessione

                while (true) //Attendo la risposta dal server
                {
                    //Leggo la risposta del server
                    int size = in.readInt();
                    byte[] buff = new byte[size];
                    int len = in.read(buff, 0, size);
                    String answer = new String(buff, 0, len); //La converto in testo

                    boolean endingMessage = parseOutput(answer); //Eseguo il parsing del messaggio
                    if(endingMessage) break; //Se era l'ultimo messaggio che stavo aspettando interrompo il ciclo
                }
            }
            //Interrompo le connessioni e chiudo le stream
            notificationHandler.stopListener();
            out.close();
            in.close();
        } catch (IOException e) {
            System.out.println("Impossibile connettersi al server o avviare il servizio di notifiche");
        }
    }

    //Funzione che converte l'input dell'utente in un messaggio valido da mandare al server
    private static MessageType parseInput(String input)
    {
        String[] parts = input.split("\\("); //Divido da dove iniziano i parametri del comando
        if(parts.length == 0) return null; //Se l'input è vuoto ritorno null
        try{
            String[] params = new String[0];
            //Rimuovo tutti i caratteri extra e divido per il delimitatore ',' per avere l'elenco dei parametri
            if(parts.length > 1) params = parts[1].replace(")", "").replace(", ", ",").trim().split(",");
            int paramNum = params.length;
            switch (Operation.valueOf(parts[0].trim())) //Controllo se il comando è una delle operazioni supportate (IllegalArgumentException altrimenti)
            {
                //Per ogni operazione creo il relativo messaggio con i parametri corretti
                case Operation.exit:
                    if(paramNum != 0) throw new IncorrectParameterException();
                    return new Exit();
                case Operation.register:
                    if(paramNum != 2) throw new IncorrectParameterException();
                    return new Registration(params[0], params[1]);
                case Operation.updateCredentials:
                    if(paramNum != 3) throw new IncorrectParameterException();
                    return new UpdateCredentials(params[0], params[1], params[2]);
                case Operation.login:
                    if(paramNum != 2) throw new IncorrectParameterException();
                    return new Login(params[0], params[1]);
                case Operation.logout:
                    if(paramNum != 0) throw new IncorrectParameterException();
                    return new Logout();
                case Operation.insertLimitOrder:
                    if(paramNum != 3) throw new IncorrectParameterException();
                    return new InsertLimitOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2]));
                case Operation.insertMarketOrder:
                    if(paramNum != 2) throw new IncorrectParameterException();
                    return new InsertMarketOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]));
                case Operation.insertStopOrder:
                    if(paramNum != 3) throw new IncorrectParameterException();
                    return new InsertStopOrder(OrderType.valueOf(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2]));
                case Operation.cancelOrder:
                    if(paramNum != 1) throw new IncorrectParameterException();
                    return new CancelOrder(Integer.parseInt(params[0]));
                case Operation.getPriceHistory:
                    if(paramNum != 1) throw new IncorrectParameterException();
                    Calendar time = getTimeFromMonth(params[0]); //Converto l'input in un oggetto Calendar
                    time.setTimeZone(TimeZone.getTimeZone("GMT"));
                    SimpleDateFormat sdf = new SimpleDateFormat("MMyyyy"); //Imposto il formato in cui convertire la data
                    return new GetPriceHistory(sdf.format(time.getTime())); //Invio la data formattata al server
                default:
                    System.out.println("Comando sconosciuto");
                    break;
            }
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Comando sconosciuto");
        }
        catch (ArrayIndexOutOfBoundsException | IncorrectParameterException e)
        {
            System.out.println("Errore nei parametri del comando");
        }
        return null; //Ritorno null se l'input non è stato convertito in un messaggio
    }

    private static Calendar getTimeFromMonth(String month) throws IncorrectParameterException, IllegalArgumentException{
        Calendar time = Calendar.getInstance();
        //Leggo il mese come numero (da 1 a 12) (IllegalArgumentException se la stringa non contiene un numero valido)
        int monthNum = Integer.parseInt(month);
        if(monthNum < 1 || monthNum > 12) throw new IncorrectParameterException();
        time.set(Calendar.MONTH, monthNum - 1); //Converto il mese a 0-11
        time.set(Calendar.YEAR, 2024); //Imposto l'anno 2024 perché i dati forniti per il progetto riguardano l'anno 2024
        return time;
    }

    //Funzione che traduce la risposta del server in testo per l'utente e controlla se il messaggio arrivato è l'ultimo per la richiesta precedentemente eseguita
    private static boolean parseOutput(String answer)
    {
        JsonElement element = JsonParser.parseString(answer); //Inizio a leggere l'oggetto come json
        if(element.getAsJsonObject().get("data") != null) //Se contiene un campo 'data', allora la risposta è una PriceResponse
        {
            PriceResponse priceResponse = gson.fromJson(answer, PriceResponse.class); //De-serializzo in PriceResponse
            for(PriceHistory history: priceResponse.data) //Leggo le PriceHistory dei giorni che ho ricevuto (max 8)
            {
                System.out.printf("Day %d: opening: %.2f USD, closing: %.2f USD, max: %.2f USD, min: %.2f USD\n", history.day, history.openingPrice / 1000.f, history.closingPrice / 1000.f, history.maxPrice / 1000.f, history.minPrice / 1000.f);
            }
            return false; //Le PriceResponse non sono mai l'ultimo messaggio (si concludono con una Standard Response con codice di successo o di errore)
        }
        else if(element.getAsJsonObject().get("orderId") != null) //Se contiene un campo 'orderId', allora la risposta è una OrderResponse
        {
            OrderResponse orderResponse = gson.fromJson(answer, OrderResponse.class); //De-serializzo in OrderResponse
            //Comunico all'utente l'esito dell'ordine
            if(orderResponse.orderId == -1) System.out.println("Non è stato possibile effettuare l'ordine");
            else System.out.printf("Ordine %d piazzato con successo\n", orderResponse.orderId);
        }
        else if(element.getAsJsonObject().get("response") != null) //Se contiene un campo 'response', allora la risposta è una StandardResponse
        {
            StandardResponse responseMessage = gson.fromJson(answer, StandardResponse.class); //De-serializzo in StandardResponse
            System.out.printf("Risposta %d: %s\n", responseMessage.response, responseMessage.errorMessage); //Comunico la risposta all'utente
        }
        System.out.printf("RAW MESSAGE: [%s]\n", answer); //Stampo il testo RAW a scopo informativo per il progetto
        return true;
    }

    //Funzione che legge la porta e l'host da usare per TCP
    private static void readConfig() throws IOException {
        InputStream input = ClientMain.class.getResourceAsStream(configFile);
        if(input == null) throw new IOException();

        Properties prop = new Properties();
        prop.load(input);
        tcp_port = Integer.parseInt(prop.getProperty("TCP_port"));
        host = prop.getProperty("host");
        input.close();
    }
}