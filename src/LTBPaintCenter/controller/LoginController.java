package LTBPaintCenter.controller;

import LTBPaintCenter.dao.AdminDAO;
import LTBPaintCenter.view.LoginPanel;
import LTBPaintCenter.model.Global;
import javax.swing.*;

/**
 * This controller handles user login functionality.
 * It manages the login screen and authenticates users (admin or cashier).
 * After successful login, it launches the main application.
 */
public class LoginController {
    
    private final LoginPanel view;
    private final JFrame frame;

    /**
     * Constructor - creates the login window and sets up event handlers.
     */
    public LoginController() {
        view = new LoginPanel();
        frame = new JFrame("LTB Paint Center — Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(view);
        frame.pack();
        frame.setLocationRelativeTo(null);  // Center the window

        attach();
    }

    /**
     * Attaches event handlers to the login button.
     */
    private void attach() {
        view.getBtnLogin().addActionListener(e -> {
            String username = view.getTfUser().getText().trim();
            String password = new String(view.getPfPass().getPassword());

            // Validate username
            if (username.isBlank()) {
                JOptionPane.showMessageDialog(frame, "Enter username", "Login", 
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean authenticated = false;

            // Check if user is admin
            if ("admin".equalsIgnoreCase(username)) {
                // Verify admin password from database
                authenticated = AdminDAO.verifyPassword(password);
                if (!authenticated) {
                    JOptionPane.showMessageDialog(frame, "Invalid admin password", 
                            "Login failed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            // Check if user is cashier
            else if ("cashier".equalsIgnoreCase(username)) {
                // Demo cashier password (hardcoded for simplicity)
                if (!"cashier123".equals(password)) {
                    JOptionPane.showMessageDialog(frame, "Invalid cashier password", 
                            "Login failed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                authenticated = true;
            } else {
                JOptionPane.showMessageDialog(frame, "Unknown username", 
                        "Login failed", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Login successful - set global state
            Global.currentUser = username;
            Global.isAdminMode = "admin".equalsIgnoreCase(username);

            // Launch main application on the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                frame.dispose();  // Close login window

                MainController mainController = new MainController();

                // If admin logged in, enable admin mode in the main frame
                if (Global.isAdminMode) {
                    try {
                        var frameObj = mainController.getFrame();
                        if (frameObj != null) {
                            frameObj.setAdminMode(true);
                        }
                    } catch (Exception ex) {
                        // Ignore errors if setAdminMode doesn't exist
                    }
                }
            });
        });
    }

    /**
     * Shows the login window.
     */
    public void show() {
        frame.setVisible(true);
    }
}
