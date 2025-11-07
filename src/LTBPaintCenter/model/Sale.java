package LTBPaintCenter.model;

import java.util.*;

/**
 * This class represents a sale/transaction.
 * It contains a list of items sold, the sale ID (reference number),
 * the date/time of the sale, and the total amount.
 */
public class Sale {
    
    private final String saleId;  // Unique reference number for this sale
    private final Date date;      // When the sale occurred
    private final List<SaleItem> items = new ArrayList<>();  // List of items in this sale
    private double total;  // Total amount of the sale

    /**
     * Constructor - creates a new sale with current date/time.
     * 
     * @param saleId The unique reference number for this sale
     */
    public Sale(String saleId) {
        this.saleId = saleId;
        this.date = new Date();
    }

    /**
     * Constructor - creates a sale with a specific date/time.
     * Used when loading sales from the database.
     * 
     * @param saleId The unique reference number
     * @param date The date/time of the sale
     */
    public Sale(String saleId, Date date) {
        this.saleId = saleId;
        this.date = (date != null) ? date : new Date();
    }

    /**
     * Adds an item to this sale.
     * Automatically updates the total.
     * 
     * @param item The SaleItem to add
     */
    public void addItem(SaleItem item) { 
        items.add(item); 
        total += item.getSubtotal(); 
    }
    
    public String getId() { 
        return saleId; 
    }
    
    public Date getDate() { 
        return date; 
    }
    
    public double getTotal() { 
        return total; 
    }
    
    public List<SaleItem> getItems() { 
        return items; 
    }
}
