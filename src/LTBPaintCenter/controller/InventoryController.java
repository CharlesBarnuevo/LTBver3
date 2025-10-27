package LTBPaintCenter.controller;

import LTBPaintCenter.dao.InventoryDAO;
import LTBPaintCenter.model.Database;
import LTBPaintCenter.model.InventoryBatch;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller layer between InventoryDAO and the UI panels.
 * Handles logic for adding, updating, deleting, and filtering inventory items.
 */
public class InventoryController {

    private final InventoryDAO inventoryDAO;
    private final LTBPaintCenter.view.InventoryPanel view;

    public InventoryController() {
        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        inventoryDAO = new InventoryDAO(conn);
        this.view = new LTBPaintCenter.view.InventoryPanel(this);
    }

    // ───────────────────────────────
    // CRUD operations
    // ───────────────────────────────

    public boolean addBatch(String productCode, String name, String brand, String color, String type, double price, int qty,
                            LocalDate dateImported, LocalDate expirationDate) {

        String status = determineStatus(expirationDate, qty);
        InventoryBatch batch = new InventoryBatch(0, productCode, name, brand, color, type, price, qty, dateImported, expirationDate, status);
        return inventoryDAO.addBatch(batch);
    }

    public List<InventoryBatch> getAllBatches() {
        inventoryDAO.refreshStatuses();
        return inventoryDAO.getAllBatches();
    }

    public boolean updateBatch(InventoryBatch batch) {
        batch.setStatus(determineStatus(batch.getExpirationDate(), batch.getQuantity()));
        return inventoryDAO.updateBatch(batch);
    }

    public boolean deleteBatch(int id) {
        return inventoryDAO.deleteBatch(id);
    }

    // ───────────────────────────────
    // POS filtering
    // ───────────────────────────────
    public List<InventoryBatch> getAvailableForPOS() {
        inventoryDAO.refreshStatuses();
        List<InventoryBatch> all = inventoryDAO.getAllBatches();
        return all.stream()
                .filter(b -> !"Expired".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getQuantity() > 0)
                .toList();
    }

    // ───────────────────────────────
    // Helper: determine status
    // ───────────────────────────────
    private String determineStatus(LocalDate expirationDate, int qty) {
        LocalDate today = LocalDate.now();
        if (expirationDate != null) {
            if (!expirationDate.isAfter(today)) {
                return "Expired"; // expiration <= today
            } else if (expirationDate.isBefore(today.plusDays(7))) {
                return "Expiring Soon"; // within next 7 days
            }
        }
        if (qty <= 0) {
            return "Out of Stock";
        } else if (qty <= 5) {
            return "Low Stock";
        } else {
            return "Active";
        }
    }

    // ───────────────────────────────
    // Log updates for MonitoringPanel
    // ───────────────────────────────
    public String generateStatusLogs() {
        StringBuilder logs = new StringBuilder();
        List<InventoryBatch> batches = getAllBatches();
        LocalDate today = LocalDate.now();

        for (InventoryBatch b : batches) {
            if ("Expired".equalsIgnoreCase(b.getStatus())) {
                logs.append(String.format("[%s] %s (%s) expired on %s%n",
                        today, b.getName(), b.getBrand(), b.getExpirationDate()));
            } else if ("Expiring Soon".equalsIgnoreCase(b.getStatus())) {
                long daysLeft = b.getExpirationDate().toEpochDay() - today.toEpochDay();
                logs.append(String.format("[%s] %s (%s) expiring in %d days (%s)%n",
                        today, b.getName(), b.getBrand(), daysLeft, b.getExpirationDate()));
            } else if ("Low Stock".equalsIgnoreCase(b.getStatus())) {
                logs.append(String.format("[%s] %s (%s) low on stock — %d left%n",
                        today, b.getName(), b.getBrand(), b.getQuantity()));
            } else if ("Out of Stock".equalsIgnoreCase(b.getStatus())) {
                logs.append(String.format("[%s] %s (%s) is out of stock%n",
                        today, b.getName(), b.getBrand()));
            }
        }

        return logs.toString();
    }

    // ───────────────────────────────
    // View accessors for MainFrame/MainController
    // ───────────────────────────────
    public javax.swing.JPanel getView() {
        return view;
    }

    public void refreshInventory() {
        if (view != null) {
            view.refreshTable();
        }
    }
}
