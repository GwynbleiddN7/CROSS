package ClientToServer;

import Utility.Operation;

public class UpdateCredentials extends MessageType{
    public final String username;
    public final String old_password;
    public final String new_password;

    public UpdateCredentials(String username, String old_password, String new_password)
    {
        this.username = username;
        this.old_password = old_password;
        this.new_password = new_password;
    }
    @Override
    public Operation getOperation()
    {
        return Operation.updateCredentials;
    }
}
