package LTBPaintCenter.model;

import java.time.LocalDate;

/**
 * Represents a single batch of a product in the inventory.
 * Each batch may have its own expiration date, import date, and status.
 */
public class InventoryBatch {
    private int id;
    private String productCode; // user-typed string ID
    private String name;
    private String brand;
    private String color;
    private String type;
    private double price;
    private int quantity;
    private LocalDate dateImported;
    private LocalDate expirationDate;
    private String status; // "Active", "Expired", "Low Stock", etc.

    // Constructors
    public InventoryBatch() {}

    public InventoryBatch(int id, String name, String brand, String color, String type,
                          double price, int quantity, LocalDate dateImported,
                          LocalDate expirationDate, String status) {
        this(id, null, name, brand, color, type, price, quantity, dateImported, expirationDate, status);
    }

    public InventoryBatch(int id, String productCode, String name, String brand, String color, String type,
                          double price, int quantity, LocalDate dateImported,
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

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDate getDateImported() { return dateImported; }
    public void setDateImported(LocalDate dateImported) { this.dateImported = dateImported; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Utility Methods
    public boolean isExpired() {
        return expirationDate != null && LocalDate.now().isAfter(expirationDate);
    }

    public boolean isExpiringSoon() {
        if (expirationDate == null) return false;
        LocalDate now = LocalDate.now();
        return !isExpired() && !expirationDate.isBefore(now) &&
                expirationDate.isBefore(now.plusDays(7));
    }

    public boolean isLowStock() {
        return quantity <= 5; // configurable threshold
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s) - ₱%.2f x%d [%s]",
                name, brand, color, price, quantity, status);
    }
}
