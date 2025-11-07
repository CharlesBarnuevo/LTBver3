package LTBPaintCenter.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This class handles the database connection.
 * It creates and manages connections to the SQLite database.
 */
public class Database {
    
    // Path to the database file (SQLite database)
    private static final String DB_PATH = 
            System.getProperty("user.dir") + "/src/LTBPaintCenter/ltbpaintcenter.db";
    
    // JDBC URL for connecting to the SQLite database
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    /**
     * Gets a connection to the database.
     * Creates the directory if it doesn't exist.
     * 
     * @return A Connection object to the database
     * @throws SQLException If there's an error connecting to the database
     */
    public static Connection getConnection() throws SQLException {
        // Make sure the directory exists before trying to create the database
        java.io.File file = new java.io.File(DB_PATH).getParentFile();
        if (file != null && !file.exists()) {
            file.mkdirs();
        }

        System.out.println("Using database at: " + DB_PATH);
        return DriverManager.getConnection(DB_URL);
    }
}
