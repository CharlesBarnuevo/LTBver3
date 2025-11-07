package LTBPaintCenter.dao;

import LTBPaintCenter.model.Database;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class provides utility functions for managing sales transactions.
 * Used for administrative maintenance tasks like clearing all sales data.
 */
public class TransactionsDAO {

    /**
     * Clears all sales transactions from the database.
     * Also resets the AUTOINCREMENT sequence for the sales table.
     * This operation is performed in a transaction to ensure data integrity.
     * 
     * WARNING: This permanently deletes all sales records!
     */
    public static void clearAllSales() {
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement()) {
            
            try {
                // Disable autocommit so we can use a transaction
                conn.setAutoCommit(false);

                // Delete all rows from the sales table
                st.executeUpdate("DELETE FROM sales");

                // Reset AUTOINCREMENT sequence for SQLite
                try {
                    st.executeUpdate("DELETE FROM sqlite_sequence WHERE name='sales'");
                } catch (SQLException ignored) {
                    // sqlite_sequence table may not exist, which is fine
                }

                // Commit the transaction
                conn.commit();
            } catch (SQLException ex) {
                // If anything goes wrong, rollback the transaction
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                    // Ignore rollback errors
                }
                throw ex;
            } finally {
                // Always restore autocommit
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Ignore errors restoring autocommit
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear sales transactions: " + e.getMessage(), e);
        }
    }
}
