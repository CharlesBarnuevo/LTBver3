package LTBPaintCenter.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility DAO for administrative maintenance actions on transactional data.
 */
public class TransactionsDAO {

    /**
     * Clears all transaction rows from the sales table.
     * Performs the operation inside a single SQL transaction and
     * resets the AUTOINCREMENT sequence for the table.
     */
    public static void clearAllSales() {
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement()) {
            try {
                conn.setAutoCommit(false);

                // Delete all rows from child/transaction table(s)
                st.executeUpdate("DELETE FROM sales");

                // Reset AUTOINCREMENT sequence for SQLite if exists
                try {
                    st.executeUpdate("DELETE FROM sqlite_sequence WHERE name='sales'");
                } catch (SQLException ignored) {
                    // sqlite_sequence may not exist; ignore
                }

                conn.commit();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw ex;
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear sales transactions: " + e.getMessage(), e);
        }
    }
}
