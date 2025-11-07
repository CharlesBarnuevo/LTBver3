package LTBPaintCenter;

import javax.swing.SwingUtilities;
import LTBPaintCenter.controller.LoginController;
import LTBPaintCenter.model.DatabaseSetup;

/**
 * This is the main entry point of the application.
 * It initializes the database and starts the login screen.
 */
public class App {
    
    /**
     * Main method - starts the application.
     * First initializes the database, then shows the login screen.
     */
    public static void main(String[] args) {
        // Set up the database tables if they don't exist
        DatabaseSetup.initializeDatabase();

        // Start the GUI on the Event Dispatch Thread (required for Swing)
        SwingUtilities.invokeLater(() -> {
            new LoginController().show();
        });
    }
}
