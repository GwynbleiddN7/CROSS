package ServerToClient;

//Risposta Server->Client con campi 'response' ed 'erroreMessage'
public class StandardResponse extends ResponseMessage {
    public final int response;
    public final String errorMessage;
    public StandardResponse(int response, String errorMessage)
    {
        this.response = response;
        this.errorMessage = errorMessage;
    }
}
