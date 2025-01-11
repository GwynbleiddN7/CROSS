package ServerToClient;

import java.util.List;

//Risposta Server->Client con campo 'data', utilizzato esclusivamente per ritornare la priceHistory
public class PriceResponse extends ResponseMessage {
    public final List<PriceHistory> data;
    public PriceResponse(List<PriceHistory> data)
    {
        this.data = data;
    }
}
