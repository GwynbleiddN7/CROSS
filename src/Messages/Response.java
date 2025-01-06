package Messages;

public class Response extends ResponseMessage{
    public int response;
    public String errorMessage;
    public Response(int response, String errorMessage)
    {
        this.response = response;
        this.errorMessage = errorMessage;
    }
}
