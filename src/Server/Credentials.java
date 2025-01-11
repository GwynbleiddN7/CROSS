package Server;

//Struttura delle credenziali serializzate nel file credentials.json
public class Credentials
{
    public final String username;
    public String password;
    public Credentials(String username, String password)
    {
        this.username = username;
        this.password = password;
    }
}