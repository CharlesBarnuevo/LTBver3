package LTBPaintCenter.dao;

import LTBPaintCenter.model.InventoryBatch;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles all database operations for inventory batches.
 * It connects to the database and performs CRUD operations (Create, Read, Update, Delete).
 * Also handles automatic product ID generation in MMDDYYXXX format.
 */
public class InventoryDAO {
    
    private final Connection conn;

    /**
     * Constructor - stores the database connection for use in all methods
     */
    public InventoryDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Checks if a product code already exists in the database.
     * This is used to prevent duplicate product IDs.
     * 
     * @param productCode The product code to check
     * @return true if the code exists, false otherwise
     */
    private boolean productCodeExists(String productCode) {
        if (conn == null || productCode == null || productCode.trim().isEmpty()) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM inventory WHERE product_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking product code existence: " + e.getMessage());
        }
        return false;
    }

    /**
     * Generates a unique product ID in the format MMDDYYXXX.
     * Format explanation:
     * - MM = Month (2 digits, e.g., 12 for December)
     * - DD = Day (2 digits, e.g., 25 for 25th)
     * - YY = Year (2 digits, e.g., 24 for 2024)
     * - XXX = Increment number (3 digits, starts at 001, auto-increments)
     * 
     * Example: 122524001 means December 25, 2024, product #1
     * 
     * @param date The date to use for the product ID (uses today if null)
     * @return A unique product ID string in MMDDYYXXX format
     */
    public String generateProductId(LocalDate date) {
        // Use today's date if no date provided
        if (date == null) {
            date = LocalDate.now();
        }
        
        // Check if database connection is available
        if (conn == null) {
            System.err.println("ERROR: Database connection is null in generateProductId!");
            // Return fallback ID using timestamp if connection fails
            return String.format("%02d%02d%02d%03d", 
                date.getMonthValue(), 
                date.getDayOfMonth(), 
                date.getYear() % 100,
                (int)(System.currentTimeMillis() % 1000));
        }
        
        // Create the date prefix (first 6 digits: MMDDYY)
        String datePrefix = String.format("%02d%02d%02d", 
            date.getMonthValue(), 
            date.getDayOfMonth(), 
            date.getYear() % 100);
        
        // Find the highest increment number for products created on this date
        // We need to check existing product codes to avoid duplicates
        int maxIncrement = 0;
        String sql = "SELECT product_code FROM inventory WHERE product_code IS NOT NULL " +
                     "AND LENGTH(product_code) = 8 AND product_code LIKE ? ORDER BY product_code DESC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, datePrefix + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("product_code");
                    // Make sure the code matches our format (8 characters, starts with date prefix)
                    if (code != null && code.length() == 8 && code.startsWith(datePrefix)) {
                        try {
                            // Extract the last 3 digits (the increment part)
                            String incrementStr = code.substring(6);
                            int increment = Integer.parseInt(incrementStr);
                            // Keep track of the highest increment we find
                            if (increment > maxIncrement) {
                                maxIncrement = increment;
                            }
                        } catch (NumberFormatException e) {
                            // Skip codes that don't have valid numeric increment
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating product ID: " + e.getMessage());
            e.printStackTrace();
            // Return a safe fallback ID if query fails
            return datePrefix + "001";
        }
        
        // Generate the next increment number (add 1 to the max we found)
        int nextIncrement = maxIncrement + 1;
        String generatedId = datePrefix + String.format("%03d", nextIncrement);
        
        return generatedId;
    }

    /**
     * Adds a new inventory batch to the database.
     * If no product code is provided, it will be auto-generated.
     * 
     * @param batch The inventory batch to add
     * @return true if successful, false otherwise
     */
    public boolean addBatch(InventoryBatch batch) {
        if (conn == null) {
            System.err.println("ERROR: Database connection is null in addBatch!");
            return false;
        }
        
        // Auto-generate product code if not provided
        if (batch.getProductCode() == null || batch.getProductCode().trim().isEmpty()) {
            // Use the import date, or today's date if not set
            LocalDate dateToUse = batch.getDateImported() != null ? batch.getDateImported() : LocalDate.now();
            String generatedCode = generateProductId(dateToUse);
            
            // Double-check that the generated code doesn't already exist
            // This is a safety measure in case of race conditions
            if (productCodeExists(generatedCode)) {
                System.err.println("WARNING: Generated product code already exists, generating alternative");
                // Try to find a unique code by incrementing
                int maxAttempts = 10;
                int attempt = 0;
                while (productCodeExists(generatedCode) && attempt < maxAttempts) {
                    try {
                        // Extract the current increment and add 1
                        String prefix = generatedCode.substring(0, 6);
                        int currentIncrement = Integer.parseInt(generatedCode.substring(6));
                        currentIncrement++;
                        generatedCode = prefix + String.format("%03d", currentIncrement);
                        attempt++;
                    } catch (Exception e) {
                        // Fallback: use timestamp if parsing fails
                        generatedCode = generatedCode.substring(0, 6) + 
                                       String.format("%03d", (int)(System.currentTimeMillis() % 1000));
                        break;
                    }
                }
            }
            
            batch.setProductCode(generatedCode);
        }
        
        // Insert the batch into the database
        String sql = "INSERT INTO inventory (product_code, name, brand, color, type, price, qty, " +
                     "date_imported, expiration_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getProductCode());
            ps.setString(2, batch.getName());
            ps.setString(3, batch.getBrand());
            ps.setString(4, batch.getColor());
            ps.setString(5, batch.getType());
            ps.setDouble(6, batch.getPrice());
            ps.setInt(7, batch.getQuantity());
            ps.setDate(8, batch.getDateImported() != null ? Date.valueOf(batch.getDateImported()) : null);
            ps.setDate(9, batch.getExpirationDate() != null ? Date.valueOf(batch.getExpirationDate()) : null);
            ps.setString(10, batch.getStatus());
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting batch: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all inventory batches from the database.
     * 
     * @return A list of all inventory batches, ordered by ID
     */
    public List<InventoryBatch> getAllBatches() {
        List<InventoryBatch> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY id ASC";
        
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extractBatch(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving batches: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates an existing inventory batch in the database.
     * 
     * @param batch The inventory batch with updated information
     * @return true if successful, false otherwise
     */
    public boolean updateBatch(InventoryBatch batch) {
        String sql = "UPDATE inventory SET product_code=?, name=?, brand=?, color=?, type=?, " +
                     "price=?, qty=?, date_imported=?, expiration_date=?, status=? WHERE id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getProductCode());
            ps.setString(2, batch.getName());
            ps.setString(3, batch.getBrand());
            ps.setString(4, batch.getColor());
            ps.setString(5, batch.getType());
            ps.setDouble(6, batch.getPrice());
            ps.setInt(7, batch.getQuantity());
            ps.setDate(8, batch.getDateImported() != null ? Date.valueOf(batch.getDateImported()) : null);
            ps.setDate(9, batch.getExpirationDate() != null ? Date.valueOf(batch.getExpirationDate()) : null);
            ps.setString(10, batch.getStatus());
            ps.setInt(11, batch.getId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating batch: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes an inventory batch from the database.
     * 
     * @param id The ID of the batch to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteBatch(int id) {
        String sql = "DELETE FROM inventory WHERE id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting batch: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper method to extract an InventoryBatch object from a database ResultSet.
     * This converts the database row into a Java object.
     * 
     * @param rs The ResultSet containing the database row
     * @return An InventoryBatch object with data from the database
     * @throws SQLException If there's an error reading from the database
     */
    private InventoryBatch extractBatch(ResultSet rs) throws SQLException {
        InventoryBatch batch = new InventoryBatch();
        batch.setId(rs.getInt("id"));
        batch.setName(rs.getString("name"));
        
        // Try to get product_code, but don't fail if column doesn't exist
        try {
            batch.setProductCode(rs.getString("product_code"));
        } catch (SQLException ignored) {
            // Column might not exist in older databases
        }
        
        batch.setBrand(rs.getString("brand"));
        batch.setColor(rs.getString("color"));
        batch.setType(rs.getString("type"));
        batch.setPrice(rs.getDouble("price"));
        batch.setQuantity(rs.getInt("qty"));
        batch.setDateImported(readLocalDate(rs, "date_imported"));
        batch.setExpirationDate(readLocalDate(rs, "expiration_date"));
        batch.setStatus(rs.getString("status"));
        
        return batch;
    }

    /**
     * Helper method to read a LocalDate from the database.
     * The database might store dates in different formats, so we try multiple methods.
     * 
     * @param rs The ResultSet to read from
     * @param column The name of the date column
     * @return A LocalDate object, or null if the date is null or can't be parsed
     * @throws SQLException If there's an error reading from the database
     */
    private LocalDate readLocalDate(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) {
            return null;
        }
        
        // Try different date formats that might be in the database
        if (val instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        
        if (val instanceof Number number) {
            // Sometimes dates are stored as numbers (milliseconds since epoch)
            long epochMillis = number.longValue();
            return java.time.Instant.ofEpochMilli(epochMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        
        // Try parsing as a string
        String dateString = val.toString().trim();
        if (dateString.isEmpty()) {
            return null;
        }
        
        // Check if it's a numeric string (milliseconds)
        if (dateString.matches("\\d+")) {
            try {
                long epochMillis = Long.parseLong(dateString);
                return java.time.Instant.ofEpochMilli(epochMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception ignored) {
                // Not a valid timestamp, continue to string parsing
            }
        }
        
        // Try parsing as ISO date string (yyyy-MM-dd)
        try {
            if (dateString.length() >= 10) {
                String datePart = dateString.substring(0, 10);
                return LocalDate.parse(datePart);
            }
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            // Last resort: try java.sql.Date.valueOf
            try {
                return java.sql.Date.valueOf(dateString).toLocalDate();
            } catch (Exception ex) {
                System.err.println("[InventoryDAO] Failed to parse date in column '" + column + "': " + dateString);
                return null;
            }
        }
    }

    /**
     * Refreshes the status of all inventory batches.
     * This checks expiration dates and stock levels to update statuses like
     * "Expired", "Expiring Soon", "Low Stock", "Out of Stock", or "Active".
     */
    public void refreshStatuses() {
        String sql = "SELECT * FROM inventory";
        
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                InventoryBatch batch = extractBatch(rs);
                
                // Determine the correct status based on expiration and quantity
                String newStatus;
                if (batch.isExpired()) {
                    newStatus = "Expired";
                } else if (batch.isExpiringSoon()) {
                    newStatus = "Expiring Soon";
                } else if (batch.isLowStock()) {
                    newStatus = "Low Stock";
                } else {
                    newStatus = "Active";
                }
                
                // Only update if status has changed
                if (!newStatus.equals(batch.getStatus())) {
                    batch.setStatus(newStatus);
                    updateBatch(batch);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error refreshing statuses: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
