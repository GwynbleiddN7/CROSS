package ServerToClient;

//Pacchetto dati incluso nella PriceResponse, con le informazioni della PriceHistory del giorno 'day'
public class PriceHistory {
    public int day;
    public int openingPrice;
    public int closingPrice;
    public int minPrice;
    public int maxPrice;

    public PriceHistory() {
        minPrice = Integer.MAX_VALUE;
        maxPrice = Integer.MIN_VALUE;
        openingPrice = -1;
        closingPrice = 0;
        day = 0;
    }
}
