package LTBPaintCenter.model;

import LTBPaintCenter.controller.InventoryController;
import LTBPaintCenter.controller.MonitoringController;
import LTBPaintCenter.controller.POSController;

/**
 * This class stores global variables that are used throughout the application.
 * It acts as a central place to access controllers and shared data.
 * Using static variables makes it easy to access these from anywhere in the app.
 */
public class Global {
    
    // Main data models shared across the application
    public static Inventory inventory;
    public static Report report;

    // Controllers that manage different parts of the application
    public static POSController posController;
    public static InventoryController inventoryController;
    public static MonitoringController monitoringController;

    // User session information
    public static String currentUser = null;
    public static boolean isAdminMode = false;

    /**
     * Checks if the current user is logged in as an admin.
     * 
     * @return true if admin mode is enabled, false otherwise
     */
    public static boolean isAdmin() {
        return isAdminMode;
    }

    /**
     * Resets the user session.
     * Clears the current user and disables admin mode.
     * Called when user logs out.
     */
    public static void resetSession() {
        currentUser = null;
        isAdminMode = false;
    }
}
