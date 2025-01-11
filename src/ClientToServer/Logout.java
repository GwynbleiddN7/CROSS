package ClientToServer;

import Utility.Operation;

//Messaggio Client->Server per il logout di un utente
public class Logout extends MessageType{
    @Override
    public Operation getOperation()
    {
        return Operation.logout;
    }
}
