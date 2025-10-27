package LTBPaintCenter.model;

import java.time.LocalDate;

public class Product {
    private int id;
    private String name;
    private double price;
    private int quantity;
    private String brand;
    private String color;
    private String type;
    private LocalDate dateImported;
    private LocalDate expirationDate;
    private String status;


    public Product(int id, String name, double price, int quantity, String brand, String color, String type, LocalDate dataImported, LocalDate expirationDate, String status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.brand = brand;
        this.color = color;
        this.type = type;
        this.dateImported = dataImported;
        this.expirationDate = expirationDate;
        this.status = status;
    }

    // getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDateImported() { return dateImported; }
    public void setDateImported(LocalDate dateImported) { this.dateImported = dateImported; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }


    // helper to get formatted display id like "P001"
    public String getDisplayId() {
        return String.format("P%03d", id);
    }
}
