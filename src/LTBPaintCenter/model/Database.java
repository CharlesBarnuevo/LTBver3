package LTBPaintCenter.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String DB_PATH =
            System.getProperty("user.dir") + "/src/LTBPaintCenter/ltbpaintcenter.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    public static Connection getConnection() throws SQLException {
        // Ensure directory exists
        java.io.File file = new java.io.File(DB_PATH).getParentFile();
        if (file != null && !file.exists()) file.mkdirs();

        System.out.println("Using database at: " + DB_PATH);
        return DriverManager.getConnection(DB_URL);
    }
}
