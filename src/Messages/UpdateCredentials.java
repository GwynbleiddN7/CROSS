package Messages;

public class UpdateCredentials extends MessageType{
    public String username;
    public String old_password;
    public String new_password;

    public UpdateCredentials(String username, String old_password, String new_password)
    {
        this.username = username;
        this.old_password = old_password;
        this.new_password = new_password;
    }
    @Override
    public String getOperation()
    {
        return "updateCredentials";
    }
}
