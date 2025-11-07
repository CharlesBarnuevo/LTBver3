package LTBPaintCenter.dao;

import LTBPaintCenter.model.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Utility class for generating unique sale reference numbers for receipts.
 * Uses the MMDDYYXXX pattern (month, day, year, incremental counter).
 */
public final class SaleReferenceGenerator {

    private SaleReferenceGenerator() {
    }

    /**
     * Generates the next sale reference number using the format MMDDYYXXX.
     * The numeric suffix increments for each sale recorded on the same day.
     *
     * @param date the sale date to encode (uses today when null)
     * @return a unique sale reference string in MMDDYYXXX format
     */
    public static String generateSaleReference(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        String datePrefix = String.format("%02d%02d%02d",
                date.getMonthValue(),
                date.getDayOfMonth(),
                date.getYear() % 100);

        int maxIncrement = 0;

        try (Connection conn = Database.getConnection()) {
            if (conn == null) {
                return buildFallbackId(datePrefix);
            }

            String sql = "SELECT sale_reference FROM sales " +
                    "WHERE sale_reference IS NOT NULL AND sale_reference LIKE ? " +
                    "ORDER BY sale_reference DESC";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, datePrefix + "%");

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String code = rs.getString(1);
                        if (code == null || !code.startsWith(datePrefix)) {
                            continue;
                        }

                        String suffix = code.substring(datePrefix.length());
                        if (suffix.length() != 3) {
                            continue;
                        }

                        try {
                            int increment = Integer.parseInt(suffix);
                            if (increment > maxIncrement) {
                                maxIncrement = increment;
                            }
                        } catch (NumberFormatException ignored) {
                            // Skip malformed codes
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating sale reference: " + e.getMessage());
            return datePrefix + "001";
        }

        int nextIncrement = maxIncrement + 1;
        return datePrefix + String.format("%03d", nextIncrement);
    }

    private static String buildFallbackId(String datePrefix) {
        return datePrefix + String.format("%03d", (int) (System.currentTimeMillis() % 1000));
    }
}


