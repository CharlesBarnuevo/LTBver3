package LTBPaintCenter.controller;

import LTBPaintCenter.dao.InventoryDAO;
import LTBPaintCenter.model.AdminAuthUtil;
import LTBPaintCenter.model.Database;
import LTBPaintCenter.model.InventoryBatch;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * This controller manages the inventory operations.
 * It acts as a middle layer between the user interface (InventoryPanel) 
 * and the database layer (InventoryDAO).
 * All operations require admin password authentication.
 */
public class InventoryController {

    private final InventoryDAO inventoryDAO;
    private final LTBPaintCenter.view.InventoryPanel view;

    /**
     * Constructor - sets up the database connection and creates the view
     */
    public InventoryController() {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            if (conn == null) {
                System.err.println("ERROR: Failed to get database connection!");
            } else {
                // Ensure autocommit is enabled for SQLite
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Failed to create database connection: " + e.getMessage());
            e.printStackTrace();
        }
        
        inventoryDAO = new InventoryDAO(conn);
        this.view = new LTBPaintCenter.view.InventoryPanel(this);
    }

    /**
     * Adds a new inventory batch to the system.
     * Requires admin password authentication.
     * Product code will be auto-generated if null or empty.
     * 
     * @param productCode Product ID (null or empty for auto-generation)
     * @param name Product name
     * @param brand Product brand
     * @param color Product color
     * @param type Product type
     * @param price Product price
     * @param qty Quantity in stock
     * @param dateImported Date when product was imported
     * @param expirationDate Expiration date (can be null)
     * @return true if successful, false otherwise
     */
    public boolean addBatch(String productCode, String name, String brand, String color, 
                           String type, double price, int qty, LocalDate dateImported, 
                           LocalDate expirationDate) {
        // Require admin password before allowing add operation
        boolean authenticated = AdminAuthUtil.requireAdminPasswordPopup(view);
        if (!authenticated) {
            return false;
        }

        // Determine the status based on expiration date and quantity
        String status = determineStatus(expirationDate, qty);
        
        // Create the batch object (product code will be auto-generated in DAO if null)
        InventoryBatch batch = new InventoryBatch(0, productCode, name, brand, color, type, 
                                                   price, qty, dateImported, expirationDate, status);
        
        return inventoryDAO.addBatch(batch);
    }
    
    /**
     * Generates a preview of what the product ID would be for a given date.
     * This is used by the UI to show users what ID will be generated.
     * 
     * @param date The date to generate the ID for
     * @return A product ID string in MMDDYYXXX format
     */
    public String generateProductIdPreview(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return inventoryDAO.generateProductId(date);
    }

    /**
     * Gets all inventory batches from the database.
     * Automatically refreshes statuses before returning.
     * 
     * @return A list of all inventory batches
     */
    public List<InventoryBatch> getAllBatches() {
        inventoryDAO.refreshStatuses();
        return inventoryDAO.getAllBatches();
    }

    /**
     * Updates an existing inventory batch.
     * Requires admin password authentication.
     * 
     * @param batch The batch with updated information
     * @return true if successful, false otherwise
     */
    public boolean updateBatch(InventoryBatch batch) {
        // Require admin password before allowing update operation
        boolean authenticated = AdminAuthUtil.requireAdminPasswordPopup(view);
        if (!authenticated) {
            return false;
        }

        // Update the status based on current expiration and quantity
        batch.setStatus(determineStatus(batch.getExpirationDate(), batch.getQuantity()));
        
        return inventoryDAO.updateBatch(batch);
    }

    /**
     * Deletes an inventory batch from the system.
     * Requires admin password authentication.
     * 
     * @param id The ID of the batch to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteBatch(int id) {
        // Require admin password before allowing delete operation
        boolean authenticated = AdminAuthUtil.requireAdminPasswordPopup(view);
        if (!authenticated) {
            return false;
        }

        return inventoryDAO.deleteBatch(id);
    }

    /**
     * Gets all batches that are available for Point of Sale (POS).
     * Filters out expired products and products with zero quantity.
     * 
     * @return A list of available batches for POS
     */
    public List<InventoryBatch> getAvailableForPOS() {
        inventoryDAO.refreshStatuses();
        List<InventoryBatch> allBatches = inventoryDAO.getAllBatches();
        
        // Filter to only include non-expired products with quantity > 0
        return allBatches.stream()
                .filter(batch -> !"Expired".equalsIgnoreCase(batch.getStatus()))
                .filter(batch -> batch.getQuantity() > 0)
                .toList();
    }

    /**
     * Determines the status of a product based on expiration date and quantity.
     * Statuses can be: "Expired", "Expiring Soon", "Out of Stock", "Low Stock", or "Active"
     * 
     * @param expirationDate The expiration date (can be null)
     * @param qty The quantity in stock
     * @return A status string
     */
    private String determineStatus(LocalDate expirationDate, int qty) {
        LocalDate today = LocalDate.now();
        
        // Check expiration status first
        if (expirationDate != null) {
            // Product is expired if expiration date is today or in the past
            if (!expirationDate.isAfter(today)) {
                return "Expired";
            }
            // Product is expiring soon if expiration is within the next 7 days
            if (expirationDate.isBefore(today.plusDays(7))) {
                return "Expiring Soon";
            }
        }
        
        // Check stock level
        if (qty <= 0) {
            return "Out of Stock";
        } else if (qty <= 5) {
            return "Low Stock";
        } else {
            return "Active";
        }
    }

    /**
     * Generates status logs for the monitoring system.
     * Creates formatted log messages for expired, expiring, low stock, and out of stock items.
     * 
     * @return A string containing formatted log messages
     */
    public String generateStatusLogs() {
        StringBuilder logs = new StringBuilder();
        List<InventoryBatch> batches = getAllBatches();
        LocalDate today = LocalDate.now();

        for (InventoryBatch batch : batches) {
            if ("Expired".equalsIgnoreCase(batch.getStatus())) {
                logs.append(String.format("[%s] %s (%s) expired on %s%n",
                        today, batch.getName(), batch.getBrand(), batch.getExpirationDate()));
            } else if ("Expiring Soon".equalsIgnoreCase(batch.getStatus())) {
                long daysLeft = batch.getExpirationDate().toEpochDay() - today.toEpochDay();
                logs.append(String.format("[%s] %s (%s) expiring in %d days (%s)%n",
                        today, batch.getName(), batch.getBrand(), daysLeft, batch.getExpirationDate()));
            } else if ("Low Stock".equalsIgnoreCase(batch.getStatus())) {
                logs.append(String.format("[%s] %s (%s) low on stock — %d left%n",
                        today, batch.getName(), batch.getBrand(), batch.getQuantity()));
            } else if ("Out of Stock".equalsIgnoreCase(batch.getStatus())) {
                logs.append(String.format("[%s] %s (%s) is out of stock%n",
                        today, batch.getName(), batch.getBrand()));
            }
        }

        return logs.toString();
    }

    /**
     * Gets the view (UI panel) for this controller.
     * 
     * @return The InventoryPanel view
     */
    public javax.swing.JPanel getView() {
        return view;
    }

    /**
     * Refreshes the inventory table in the view.
     * This updates the display to show the latest data.
     */
    public void refreshInventory() {
        if (view != null) {
            view.refreshTable();
        }
    }
}
