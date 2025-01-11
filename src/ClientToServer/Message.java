package ClientToServer;

import Utility.Operation;

//Messaggio Client->Server effettivo che verrà serializzato e inviato. Incorpora il nome dell'operazione e i dati da mandare al server
public class Message<T extends MessageType> {
    public final Operation operation;
    public final T values;
    public Message(T values)
    {
        this.values = values;
        operation = values.getOperation();
    }
}
