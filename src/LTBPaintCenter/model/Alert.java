package LTBPaintCenter.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Alert {
    public enum AlertType {
        EXPIRING_SOON, EXPIRED, LOW_STOCK, OUT_OF_STOCK
    }

    private final int batchId;
    private final String productName;
    private final String brand;
    private final AlertType type;
    private final String message;
    private final LocalDate dateGenerated;

    public Alert(int batchId, String productName, String brand, AlertType type, String message) {
        this.batchId = batchId;
        this.productName = productName;
        this.brand = brand;
        this.type = type;
        this.message = message;
        this.dateGenerated = LocalDate.now();
    }

    // Factory methods for convenience
    public static Alert checkBatch(ProductBatch batch) {
        if (batch == null) return null;

        // Expiration checks
        if (batch.getExpirationDate() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpirationDate());

            if (daysLeft < 0) {
                return new Alert(batch.getId(), batch.getName(), batch.getBrand(),
                        AlertType.EXPIRED,
                        String.format("Batch of %s (%s) has expired on %s.",
                                batch.getName(), batch.getBrand(), batch.getExpirationDate()));
            } else if (daysLeft <= 7) {
                return new Alert(batch.getId(), batch.getName(), batch.getBrand(),
                        AlertType.EXPIRING_SOON,
                        String.format("Batch of %s (%s) is expiring in %d day(s) on %s.",
                                batch.getName(), batch.getBrand(), daysLeft, batch.getExpirationDate()));
            }
        }

        // Stock checks
        if (batch.getQuantity() == 0) {
            return new Alert(batch.getId(), batch.getName(), batch.getBrand(),
                    AlertType.OUT_OF_STOCK,
                    String.format("Batch of %s (%s) is out of stock!", batch.getName(), batch.getBrand()));
        } else if (batch.getQuantity() <= 5) { // threshold for low stock
            return new Alert(batch.getId(), batch.getName(), batch.getBrand(),
                    AlertType.LOW_STOCK,
                    String.format("Batch of %s (%s) is running low — only %d left!",
                            batch.getName(), batch.getBrand(), batch.getQuantity()));
        }

        return null;
    }

    // --- Getters ---
    public int getBatchId() { return batchId; }
    public String getProductName() { return productName; }
    public String getBrand() { return brand; }
    public AlertType getType() { return type; }
    public String getMessage() { return message; }
    public LocalDate getDateGenerated() { return dateGenerated; }

    @Override
    public String toString() {
        return String.format("[%s] %s — %s", type, productName, message);
    }
}
