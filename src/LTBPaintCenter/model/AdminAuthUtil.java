package LTBPaintCenter.model;

import LTBPaintCenter.dao.AdminDAO;
import javax.swing.*;
import java.awt.*;

/**
 * This utility class handles admin password authentication.
 * It shows a password dialog and verifies the password against the database.
 * All methods are static since this is a utility class.
 */
public final class AdminAuthUtil {
    
    // Private constructor prevents instantiation (utility class)
    private AdminAuthUtil() {
    }

    /**
     * Shows a password dialog and verifies the admin password.
     * This is required before performing sensitive operations like
     * adding, updating, or deleting inventory items.
     * 
     * @param parent The parent component for the dialog (usually a JFrame or JPanel)
     * @return true if password is correct, false otherwise
     */
    public static boolean requireAdminPasswordPopup(Component parent) {
        // Create a password field for user input
        JPasswordField passwordField = new JPasswordField();
        
        // Show the password dialog
        int option = JOptionPane.showConfirmDialog(
                parent, 
                passwordField, 
                "Admin password required", 
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.PLAIN_MESSAGE);
        
        // If user cancelled, return false
        if (option != JOptionPane.OK_OPTION) {
            return false;
        }
        
        // Get the password from the field
        String password = new String(passwordField.getPassword());
        
        // Verify the password against the database
        boolean isValid = AdminDAO.verifyPassword(password);
        
        // If password is wrong, show an error message
        if (!isValid) {
            JOptionPane.showMessageDialog(
                    parent, 
                    "Invalid admin password.", 
                    "Access denied", 
                    JOptionPane.WARNING_MESSAGE);
        }
        
        return isValid;
    }
}
