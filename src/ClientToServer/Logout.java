package ClientToServer;

import Utility.Operation;

public class Logout extends MessageType{
    @Override
    public Operation getOperation()
    {
        return Operation.logout;
    }
}
