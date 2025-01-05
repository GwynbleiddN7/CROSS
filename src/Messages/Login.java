package Messages;

public class Login extends MessageType{
    public String username;
    public String password;
    public Login(String username, String password)
    {
        this.username = username;
        this.password = password;
    }
    @Override
    public String getOperation()
    {
        return "login";
    }
}
