package LTBPaintCenter.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * This class represents an alert/warning about a product batch.
 * Alerts can be for expiring products, expired products, low stock, or out of stock.
 */
public class Alert {
    
    /**
     * Types of alerts that can be generated.
     */
    public enum AlertType {
        EXPIRING_SOON,  // Product expiring within 7 days
        EXPIRED,        // Product has expired
        LOW_STOCK,      // Product has 5 or fewer items
        OUT_OF_STOCK    // Product has 0 items
    }

    private final int batchId;
    private final String productName;
    private final String brand;
    private final AlertType type;
    private final String message;
    private final LocalDate dateGenerated;

    /**
     * Constructor - creates a new alert.
     * 
     * @param batchId The ID of the batch this alert is for
     * @param productName The name of the product
     * @param brand The brand of the product
     * @param type The type of alert
     * @param message The alert message to display
     */
    public Alert(int batchId, String productName, String brand, AlertType type, String message) {
        this.batchId = batchId;
        this.productName = productName;
        this.brand = brand;
        this.type = type;
        this.message = message;
        this.dateGenerated = LocalDate.now();
    }

    /**
     * Factory method - checks a product batch and creates an alert if needed.
     * Checks for expiration, expiring soon, low stock, and out of stock conditions.
     * 
     * @param batch The ProductBatch to check
     * @return An Alert if issues are found, null otherwise
     */
    public static Alert checkBatch(ProductBatch batch) {
        if (batch == null) {
            return null;
        }

        // Check expiration status
        if (batch.getExpirationDate() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpirationDate());

            // Product has expired
            if (daysLeft < 0) {
                return new Alert(batch.getId(), batch.getName(), batch.getBrand(),
                        AlertType.EXPIRED,
                        String.format("Batch of %s (%s) has expired on %s.",
                                batch.getName(), batch.getBrand(), batch.getExpirationDate()));
            }
            // Product expiring soon (within 7 days)
            else if (daysLeft <= 7) {
                return new Alert(batch.getId(), batch.getName(), batch.getBrand(),
                        AlertType.EXPIRING_SOON,
                        String.format("Batch of %s (%s) is expiring in %d day(s) on %s.",
                                batch.getName(), batch.getBrand(), daysLeft, batch.getExpirationDate()));
            }
        }

        // Check stock levels
        if (batch.getQuantity() == 0) {
            return new Alert(batch.getId(), batch.getName(), batch.getBrand(),
                    AlertType.OUT_OF_STOCK,
                    String.format("Batch of %s (%s) is out of stock!", 
                            batch.getName(), batch.getBrand()));
        } else if (batch.getQuantity() <= 5) {
            return new Alert(batch.getId(), batch.getName(), batch.getBrand(),
                    AlertType.LOW_STOCK,
                    String.format("Batch of %s (%s) is running low — only %d left!",
                            batch.getName(), batch.getBrand(), batch.getQuantity()));
        }

        return null;
    }

    // Getter methods
    
    public int getBatchId() { 
        return batchId; 
    }
    
    public String getProductName() { 
        return productName; 
    }
    
    public String getBrand() { 
        return brand; 
    }
    
    public AlertType getType() { 
        return type; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public LocalDate getDateGenerated() { 
        return dateGenerated; 
    }

    /**
     * Returns a string representation of the alert.
     * 
     * @return A formatted string with alert information
     */
    @Override
    public String toString() {
        return String.format("[%s] %s — %s", type, productName, message);
    }
}
