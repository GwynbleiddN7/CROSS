package ClientToServer;

import Utility.Operation;

//Messaggio Client->Server per l'interruzione della connessione
public class Exit extends MessageType{
    @Override
    public Operation getOperation()
    {
        return Operation.exit;
    }
}
