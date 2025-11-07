package LTBPaintCenter.model;

/**
 * This class represents a single item in a sale.
 * It stores the product information and quantity purchased.
 */
public class SaleItem {
    
    private final int productId;  // The ID of the product being sold
    private final String name;    // The name of the product
    private final double price;   // The price per unit
    private int qty;              // The quantity being purchased

    /**
     * Constructor - creates a new sale item.
     * 
     * @param productId The ID of the product
     * @param name The name of the product
     * @param price The price per unit
     * @param qty The quantity being purchased
     */
    public SaleItem(int productId, String name, double price, int qty) {
        this.productId = productId;
        this.name = name != null ? name : "Unnamed";
        this.price = price;
        this.qty = Math.max(qty, 0);  // Make sure quantity is never negative
    }

    /**
     * Adds more quantity to this item.
     * 
     * @param additional The amount to add
     */
    public void addQuantity(int additional) {
        this.qty = Math.max(0, this.qty + additional);
    }

    /**
     * Calculates the subtotal (price × quantity).
     * Rounds to 2 decimal places for currency display.
     * 
     * @return The subtotal amount
     */
    public double getSubtotal() {
        return Math.round(price * qty * 100.0) / 100.0;
    }

    public int getProductId() { 
        return productId; 
    }

    public String getName() { 
        return name; 
    }

    public int getQty() { 
        return qty; 
    }

    public double getPrice() { 
        return price; 
    }

    /**
     * Sets the quantity.
     * Ensures quantity is never negative.
     * 
     * @param qty The new quantity
     */
    public void setQty(int qty) {
        this.qty = Math.max(qty, 0);
    }

    /**
     * Returns a string representation of this sale item.
     * 
     * @return A formatted string with item information
     */
    @Override
    public String toString() {
        return String.format("%s (x%d) - ₱%.2f", name, qty, getSubtotal());
    }
}
