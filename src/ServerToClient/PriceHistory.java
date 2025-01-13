package ServerToClient;

//Pacchetto dati incluso nella PriceResponse, con le informazioni della PriceHistory del giorno 'day'
public class PriceHistory {
    public int day;
    public double openingPrice;
    public double closingPrice;
    public double minPrice;
    public double maxPrice;

    public PriceHistory() {
        minPrice = Double.MAX_VALUE;
        maxPrice = Double.MIN_VALUE;
        openingPrice = -1;
        closingPrice = 0;
        day = 0;
    }
}
