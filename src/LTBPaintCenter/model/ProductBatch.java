package LTBPaintCenter.model;

import java.time.LocalDate;

/**
 * This class represents a product batch used by the POS (Point of Sale) system.
 * It's similar to InventoryBatch but simplified for display in the POS interface.
 * Automatically computes status based on expiration date.
 */
public class ProductBatch {
    
    private int id;
    private String name;
    private String brand;
    private String color;
    private String type;
    private double price;
    private int quantity;
    private LocalDate expirationDate;
    private String status;  // "Active" or "Expired"

    /**
     * Constructor - creates a new ProductBatch and automatically computes its status.
     */
    public ProductBatch(int id, String name, String brand, String color, String type,
                        double price, int quantity, LocalDate dateImported, 
                        LocalDate expirationDate) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.color = color;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
        this.status = computeStatus();
    }

    /**
     * Computes the status based on expiration date.
     * 
     * @return "Expired" if past expiration date, "Active" otherwise
     */
    private String computeStatus() {
        if (expirationDate == null) {
            return "Active";
        }
        return expirationDate.isBefore(LocalDate.now()) ? "Expired" : "Active";
    }

    // Getter and setter methods
    
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }

    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }

    public String getBrand() { 
        return brand; 
    }
    
    public void setBrand(String brand) { 
        this.brand = brand; 
    }

    public String getColor() { 
        return color; 
    }
    
    public void setColor(String color) { 
        this.color = color; 
    }

    public String getType() { 
        return type; 
    }
    
    public void setType(String type) { 
        this.type = type; 
    }

    public double getPrice() { 
        return price; 
    }
    
    public void setPrice(double price) { 
        this.price = price; 
    }

    public int getQuantity() { 
        return quantity; 
    }

    public LocalDate getExpirationDate() { 
        return expirationDate; 
    }

    public void setStatus(String status) { 
        this.status = status; 
    }

    // Utility methods to check product status
    
    /**
     * Checks if the product has expired.
     * 
     * @return true if expiration date has passed, false otherwise
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return expirationDate.isBefore(LocalDate.now());
    }

    /**
     * Checks if the product is expiring soon (within 7 days).
     * 
     * @return true if expiring within 7 days, false otherwise
     */
    public boolean isExpiringSoon() {
        if (expirationDate == null) {
            return false;
        }
        if (isExpired()) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(7);
        // Within the next 7 days (inclusive), not in the past
        return !expirationDate.isBefore(today) && !expirationDate.isAfter(soon);
    }

    /**
     * Returns a string representation of the batch.
     * 
     * @return A formatted string with product information
     */
    @Override
    public String toString() {
        return String.format("%s (%s) - ₱%.2f, Qty: %d, Exp: %s, Status: %s",
                name, brand, price, quantity,
                expirationDate != null ? expirationDate.toString() : "N/A",
                status);
    }
}
