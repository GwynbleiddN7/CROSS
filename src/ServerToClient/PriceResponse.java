package ServerToClient;

import java.util.List;

public class PriceResponse extends ResponseMessage {
    public final List<PriceHistory> data;
    public PriceResponse(List<PriceHistory> data)
    {
        this.data = data;
    }
}
