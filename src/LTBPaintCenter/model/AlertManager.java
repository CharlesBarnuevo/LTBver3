package LTBPaintCenter.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages alerts generated from product batches.
 * Scans for expiration, low stock, and out-of-stock warnings.
 */
public class AlertManager {
    private final List<Alert> activeAlerts = new ArrayList<>();

    /**
     * Scans all product batches and generates alerts if needed.
     * @param batches list of all product batches in inventory
     */
    public void scanInventory(List<ProductBatch> batches) {
        activeAlerts.clear();
        if (batches == null) return;

        for (ProductBatch batch : batches) {
            Alert alert = Alert.checkBatch(batch);
            if (alert != null) activeAlerts.add(alert);
        }
    }

    /**
     * Returns all currently active alerts.
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }

    /**
     * Filters alerts by type (e.g., only EXPIRING_SOON).
     */
    public List<Alert> getAlertsByType(Alert.AlertType type) {
        List<Alert> filtered = new ArrayList<>();
        for (Alert alert : activeAlerts) {
            if (alert.getType() == type) filtered.add(alert);
        }
        return filtered;
    }

    /**
     * Optional: remove expired or resolved alerts.
     */
    public void clearResolved(List<ProductBatch> batches) {
        activeAlerts.removeIf(alert -> {
            for (ProductBatch batch : batches) {
                if (batch.getId() == alert.getBatchId()) {
                    // If product is deleted or replenished
                    boolean restocked = batch.getQuantity() > 5;
                    boolean notExpired = batch.getExpirationDate() != null &&
                            batch.getExpirationDate().isAfter(LocalDate.now().plusDays(7));
                    return restocked && notExpired;
                }
            }
            return false;
        });
    }
}
