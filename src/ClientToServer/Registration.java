package ClientToServer;

import Utility.Operation;

public class Registration extends MessageType{
    public final String username;
    public final String password;
    public Registration(String username, String password)
    {
        this.username = username;
        this.password = password;
    }
    @Override
    public Operation getOperation()
    {
        return Operation.register;
    }
}
