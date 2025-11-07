package LTBPaintCenter.model;

import LTBPaintCenter.dao.AdminDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class sets up the database tables when the application starts.
 * It creates all the necessary tables if they don't exist.
 * Also handles database upgrades by adding new columns to existing tables.
 */
public class DatabaseSetup {

    /**
     * Initializes the database by creating all necessary tables.
     * If tables already exist, it tries to add any missing columns.
     * This method is safe to call multiple times.
     */
    public static void initializeDatabase() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create the inventory table (stores all products)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_code TEXT,
                    name TEXT NOT NULL,
                    brand TEXT NOT NULL,
                    color TEXT,
                    type TEXT,
                    price REAL NOT NULL,
                    qty INTEGER NOT NULL DEFAULT 0,
                    date_imported TEXT DEFAULT (DATE('now')),
                    expiration_date TEXT,
                    status TEXT DEFAULT 'Active'
                );
            """);

            // Try to add product_code column for databases created before this feature
            try {
                stmt.execute("ALTER TABLE inventory ADD COLUMN product_code TEXT");
            } catch (Exception ignored) {
                // Column already exists, which is fine
            }

            // Create the sales table (stores all transactions)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sale_reference TEXT,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    price REAL NOT NULL,
                    total REAL NOT NULL,
                    sale_date TEXT DEFAULT (DATETIME('now')),
                    FOREIGN KEY(product_id) REFERENCES inventory(id)
                );
            """);

            // Try to add sale_reference column for older databases
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN sale_reference TEXT");
            } catch (Exception ignored) {
                // Column already exists, which is fine
            }

            // Create the logs table (stores monitoring events and alerts)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    message TEXT NOT NULL,
                    log_type TEXT,
                    log_date TEXT DEFAULT (DATETIME('now'))
                );
            """);

            // Create the admin_settings table (stores admin password)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS admin_settings (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL
                );
            """);

            // Set up default admin password if the table is empty
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(1) FROM admin_settings");
                 java.sql.ResultSet rs = ps.executeQuery()) {
                
                int count = rs.next() ? rs.getInt(1) : 0;
                
                // If no admin password exists, create one with default "admin123"
                if (count == 0) {
                    String defaultPassword = "admin123";
                    String salt = AdminDAO.generateSalt();
                    String hash = AdminDAO.hashPassword(defaultPassword, salt);
                    
                    try (java.sql.PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO admin_settings (id, password_hash, salt) VALUES (1, ?, ?)")) {
                        insert.setString(1, hash);
                        insert.setString(2, salt);
                        insert.executeUpdate();
                    }
                }
            } catch (Exception ex) {
                System.err.println("Failed to initialize admin_settings: " + ex.getMessage());
            }

            System.out.println("Database successfully initialized / verified.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database setup failed: " + e.getMessage());
        }
    }
}
