package LTBPaintCenter.model;

import java.util.*;

public class Sale {
    private final String saleId;
    private final Date date;
    private final List<SaleItem> items = new ArrayList<>();
    private double total;

    public Sale(String saleId) {
        this.saleId = saleId;
        this.date = new Date();
    }

    // Overloaded constructor for reconstructing from database with known date/time
    public Sale(String saleId, Date date) {
        this.saleId = saleId;
        this.date = (date != null) ? date : new Date();
    }

    public void addItem(SaleItem it) { items.add(it); total += it.getSubtotal(); }
    public String getId() { return saleId; }
    public Date getDate() { return date; }
    public double getTotal() { return total; }
    public List<SaleItem> getItems() { return items; }
}