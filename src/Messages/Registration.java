package Messages;

public class Registration extends MessageType{
    public String username;
    public String password;
    public Registration(String username, String password)
    {
        this.username = username;
        this.password = password;
    }
    @Override
    public String getOperation()
    {
        return "register";
    }
}
