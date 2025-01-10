package ClientToServer;

import Utility.Operation;

public class Exit extends MessageType{
    @Override
    public Operation getOperation()
    {
        return Operation.exit;
    }
}
