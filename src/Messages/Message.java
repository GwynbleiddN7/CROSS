package Messages;

public class Message<T extends MessageType> {
    public String operation;
    public T values;
    public Message(T values)
    {
        this.values = values;
        operation = values.getOperation();
    }
}
