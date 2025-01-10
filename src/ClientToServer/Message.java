package ClientToServer;

import Utility.Operation;

public class Message<T extends MessageType> {
    public final Operation operation;
    public final T values;
    public Message(T values)
    {
        this.values = values;
        operation = values.getOperation();
    }
}
