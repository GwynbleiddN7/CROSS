package Messages;

public class Logout extends MessageType{
    public String username;
    public Logout(String username)
    {
        this.username = username;
    }
    @Override
    public String getOperation()
    {
        return "logout";
    }
}
