package LTBPaintCenter.model;

import java.time.LocalDate;

/**
 * This class represents a product in the inventory system.
 * It stores all the information about a single product including
 * its ID, name, price, quantity, brand, color, type, dates, and status.
 */
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

    /**
     * Constructor - creates a new Product with all its information.
     * 
     * @param id The unique ID of the product
     * @param name The name of the product
     * @param price The price of the product
     * @param quantity How many units are in stock
     * @param brand The brand name
     * @param color The color of the product
     * @param type The type/category of the product
     * @param dateImported When the product was imported/added
     * @param expirationDate When the product expires (can be null)
     * @param status The current status (Active, Expired, Low Stock, etc.)
     */
    public Product(int id, String name, double price, int quantity, String brand, 
                   String color, String type, LocalDate dateImported, 
                   LocalDate expirationDate, String status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.brand = brand;
        this.color = color;
        this.type = type;
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

    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
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

    public LocalDate getDateImported() { 
        return dateImported; 
    }
    
    public LocalDate getExpirationDate() { 
        return expirationDate; 
    }
    
    public String getStatus() { 
        return status; 
    }
}
