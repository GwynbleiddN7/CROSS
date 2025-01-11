package ClientToServer;

import Utility.Operation;

//Classe astratta, super classe di tutti i tipi di messaggi. Utilizzata per capire quale operazione un messaggio generico svolgerà
public abstract class MessageType {
    public abstract Operation getOperation();
}
