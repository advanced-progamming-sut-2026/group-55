package pvz.model.shop;

import java.time.LocalDate;

public class DailyOffer {
    private String plantName;
    private int price;
    private LocalDate date;
    private boolean purchased;

    public DailyOffer(String plantName, int price, LocalDate date) {
        this.plantName = plantName;
        this.price = price;
        this.date = date;
        this.purchased = false;
    }

    public String getPlantName() { return plantName; }
    public int getPrice() { return price; }
    public LocalDate getDate() { return date; }

    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }
}