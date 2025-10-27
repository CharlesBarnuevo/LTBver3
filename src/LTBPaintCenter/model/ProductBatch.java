package LTBPaintCenter.model;

import java.time.LocalDate;

public class ProductBatch {
    private int id;
    private String name;
    private String brand;
    private String color;
    private String type;
    private double price;
    private int quantity;
    private LocalDate dateImported;
    private LocalDate expirationDate;
    private String status; // "Active" or "Expired"

    public ProductBatch(int id, String name, String brand, String color, String type,
                        double price, int quantity, LocalDate dateImported, LocalDate expirationDate) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.color = color;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.dateImported = dateImported;
        this.expirationDate = expirationDate;
        this.status = computeStatus();
    }

    public ProductBatch(String name, String brand, String color, String type,
                        double price, int quantity, LocalDate dateImported, LocalDate expirationDate) {
        this(-1, name, brand, color, type, price, quantity, dateImported, expirationDate);
    }

    private String computeStatus() {
        if (expirationDate == null) return "Active";
        return expirationDate.isBefore(LocalDate.now()) ? "Expired" : "Active";
    }

    public void updateStatus() {
        this.status = computeStatus();
    }

    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
        updateStatus();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Convenience helpers for UI and business logic
    public boolean isExpired() {
        if (expirationDate == null) return false;
        // Keep logic consistent with computeStatus(): only before today is expired
        return expirationDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon() {
        if (expirationDate == null) return false;
        if (isExpired()) return false;
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(7);
        // Within the next 7 days (inclusive), not in the past
        return !expirationDate.isBefore(today) && !expirationDate.isAfter(soon);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - ₱%.2f, Qty: %d, Exp: %s, Status: %s",
                name, brand, price, quantity,
                expirationDate != null ? expirationDate.toString() : "N/A",
                status);
    }
}
