package LTBPaintCenter.model;

import LTBPaintCenter.controller.InventoryController;
import LTBPaintCenter.controller.MonitoringController;
import LTBPaintCenter.controller.POSController;

public class Global {
    public static Inventory inventory;
    public static Report report;

    public static POSController posController;
    public static InventoryController inventoryController;
    public static MonitoringController monitoringController;

    public static String currentUser = null;
    public static boolean isAdminMode = false;

    public static boolean isAdmin() {
        return isAdminMode;
    }

    public static void resetSession() {
        currentUser = null;
        isAdminMode = false;
    }
}
