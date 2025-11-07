package LTBPaintCenter.model;

import java.time.LocalDate;

/**
 * This class represents a single batch of a product in the inventory.
 * A batch is a group of products with the same properties.
 * Each batch can have its own expiration date, import date, and status.
 * This is used by the inventory management system.
 */
public class InventoryBatch {
    
    private int id;
    private String productCode;  // The product ID in MMDDYYXXX format
    private String name;
    private String brand;
    private String color;
    private String type;
    private double price;
    private int quantity;
    private LocalDate dateImported;
    private LocalDate expirationDate;
    private String status;  // "Active", "Expired", "Low Stock", "Out of Stock", etc.

    /**
     * Default constructor - creates an empty batch.
     */
    public InventoryBatch() {
    }

    /**
     * Constructor without product code (product code will be auto-generated).
     */
    public InventoryBatch(int id, String name, String brand, String color, String type,
                          double price, int quantity, LocalDate dateImported,
                          LocalDate expirationDate, String status) {
        this(id, null, name, brand, color, type, price, quantity, dateImported, expirationDate, status);
    }

    /**
     * Full constructor with all fields including product code.
     */
    public InventoryBatch(int id, String productCode, String name, String brand, String color, 
                          String type, double price, int quantity, LocalDate dateImported,
                          LocalDate expirationDate, String status) {
        this.id = id;
        this.productCode = productCode;
        this.name = name;
        this.brand = brand;
        this.color = color;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.dateImported = dateImported;
        this.expirationDate = expirationDate;
        this.status = status;
    }

    // Getter and setter methods for all fields
    
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }

    public String getProductCode() { 
        return productCode; 
    }
    
    public void setProductCode(String productCode) { 
        this.productCode = productCode; 
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
    
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
    }

    public LocalDate getDateImported() { 
        return dateImported; 
    }
    
    public void setDateImported(LocalDate dateImported) { 
        this.dateImported = dateImported; 
    }

    public LocalDate getExpirationDate() { 
        return expirationDate; 
    }
    
    public void setExpirationDate(LocalDate expirationDate) { 
        this.expirationDate = expirationDate; 
    }

    public String getStatus() { 
        return status; 
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
        return expirationDate != null && LocalDate.now().isAfter(expirationDate);
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
        LocalDate now = LocalDate.now();
        return !isExpired() && 
               !expirationDate.isBefore(now) && 
               expirationDate.isBefore(now.plusDays(7));
    }

    /**
     * Checks if the product is low on stock (5 or fewer items).
     * 
     * @return true if quantity is 5 or less, false otherwise
     */
    public boolean isLowStock() {
        return quantity <= 5;
    }

    /**
     * Returns a string representation of the batch.
     * Useful for debugging and display purposes.
     * 
     * @return A formatted string with product information
     */
    @Override
    public String toString() {
        return String.format("%s (%s, %s) - ₱%.2f x%d [%s]",
                name, brand, color, price, quantity, status);
    }
}
