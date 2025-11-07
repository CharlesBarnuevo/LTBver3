package LTBPaintCenter.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages alerts for the inventory system.
 * It scans product batches and generates alerts for issues like
 * expiring products, expired products, low stock, and out of stock items.
 */
public class AlertManager {
    
    private final List<Alert> activeAlerts = new ArrayList<>();

    /**
     * Scans all product batches and generates alerts for any issues found.
     * Clears previous alerts and creates new ones based on current inventory state.
     * 
     * @param batches The list of all product batches to scan
     */
    public void scanInventory(List<ProductBatch> batches) {
        activeAlerts.clear();
        if (batches == null) {
            return;
        }

        // Check each batch and create alerts if needed
        for (ProductBatch batch : batches) {
            Alert alert = Alert.checkBatch(batch);
            if (alert != null) {
                activeAlerts.add(alert);
            }
        }
    }

    /**
     * Gets all currently active alerts.
     * Returns a copy so the original list can't be modified.
     * 
     * @return A list of all active alerts
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }

    /**
     * Filters alerts by type.
     * For example, get only EXPIRING_SOON alerts.
     * 
     * @param type The type of alert to filter by
     * @return A list of alerts matching the specified type
     */
    public List<Alert> getAlertsByType(Alert.AlertType type) {
        List<Alert> filtered = new ArrayList<>();
        for (Alert alert : activeAlerts) {
            if (alert.getType() == type) {
                filtered.add(alert);
            }
        }
        return filtered;
    }

    /**
     * Removes alerts that have been resolved.
     * An alert is considered resolved if:
     * - The product has been restocked (quantity > 5)
     * - The product is no longer expiring soon (expiration > 7 days away)
     * 
     * @param batches The current list of batches to check against
     */
    public void clearResolved(List<ProductBatch> batches) {
        activeAlerts.removeIf(alert -> {
            for (ProductBatch batch : batches) {
                if (batch.getId() == alert.getBatchId()) {
                    // Check if product has been restocked
                    boolean restocked = batch.getQuantity() > 5;
                    
                    // Check if product is no longer expiring soon
                    boolean notExpiringSoon = batch.getExpirationDate() != null &&
                            batch.getExpirationDate().isAfter(LocalDate.now().plusDays(7));
                    
                    // Alert is resolved if both conditions are met
                    return restocked && notExpiringSoon;
                }
            }
            return false;
        });
    }
}
