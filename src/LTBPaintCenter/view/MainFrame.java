package LTBPaintCenter.view;

import LTBPaintCenter.controller.*;
import LTBPaintCenter.dao.AdminDAO;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the main window of the application.
 * It contains a sidebar for navigation and displays different panels
 * (POS, Inventory, Monitoring) based on user selection.
 * Also handles admin/cashier mode switching.
 */
public class MainFrame extends JFrame {
    
    private final JPanel mainPanel = new JPanel(new CardLayout());
    private final CardLayout cardLayout = (CardLayout) mainPanel.getLayout();

    private final JPanel sidebar = new JPanel(new BorderLayout());
    private final JPanel navPanel = new JPanel(new GridLayout(5, 1, 10, 10));

    private JPanel posSectionPanel;
    private JPanel inventorySectionPanel;
    private JPanel monitoringSectionPanel;
    private JPanel changePasswordSectionPanel;

    private final JButton btnPOS = new JButton("POS");
    private final JButton btnInventory = new JButton("Inventory");
    private final JButton btnMonitoring = new JButton("Monitoring");

    private final JLabel lblProfile = new JLabel("👤", SwingConstants.CENTER);
    private final JLabel lblRole = new JLabel("Cashier", SwingConstants.CENTER);

    private boolean isAdmin = false;
    private final Map<String, JPanel> panelMap = new HashMap<>();

    private final POSController posController;
    private final InventoryController inventoryController;
    private final MonitoringController monitoringController;

    private JButton changePasswordButton;

    /**
     * Constructor - creates the main window and sets up all components.
     * 
     * @param posCtrl The POS controller
     * @param invCtrl The Inventory controller
     * @param monCtrl The Monitoring controller
     */
    public MainFrame(POSController posCtrl, InventoryController invCtrl, 
                     MonitoringController monCtrl) {
        this.posController = posCtrl;
        this.inventoryController = invCtrl;
        this.monitoringController = monCtrl;

        setTitle("Product Management System for LTB Paint Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 900);
        setLocationRelativeTo(null);  // Center the window
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        initSidebar();
        add(sidebar, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);

        // Create status bar with date/time display
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 210, 210)));
        statusBar.setBackground(Color.WHITE);

        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        leftStatus.setOpaque(false);
        JLabel lblDateTime = new JLabel();
        lblDateTime.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        leftStatus.add(lblDateTime);
        statusBar.add(leftStatus, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        // Update date/time every second
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date());
            lblDateTime.setText("System Date/Time: " + now);
        });
        timer.setRepeats(true);
        timer.start();

        // Add all panels
        addPanel(posController.getView(), "POS");
        addPanel(inventoryController.getView(), "Inventory");
        addPanel(monitoringController.getView(), "Monitoring");

        // Set up button actions
        btnPOS.addActionListener(e -> showPanel("POS"));
        btnInventory.addActionListener(e -> showPanel("Inventory"));
        btnMonitoring.addActionListener(e -> showPanel("Monitoring"));

        showPanel("POS");
        updateAccess();
    }

    /**
     * Initializes the sidebar with navigation buttons and profile section.
     */
    private void initSidebar() {
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(new Color(240, 240, 240));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 210, 210)));

        // Top panel with profile icon and role
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(230, 230, 230));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblProfile.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        lblProfile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblProfile.setToolTipText("Switch to Admin (Password Protected)");
        lblProfile.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleAdminToggle();
            }
        });

        lblRole.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblRole.setForeground(Color.DARK_GRAY);

        topPanel.add(lblProfile, BorderLayout.CENTER);
        topPanel.add(lblRole, BorderLayout.SOUTH);

        // Style navigation buttons
        navPanel.setBackground(new Color(240, 240, 240));
        styleSidebarButton(btnPOS);
        styleSidebarButton(btnInventory);
        styleSidebarButton(btnMonitoring);

        // Navigation buttons panel
        JPanel navButtonsPanel = new JPanel();
        navButtonsPanel.setOpaque(false);
        navButtonsPanel.setLayout(new GridLayout(3, 1, 10, 10));
        navButtonsPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        navButtonsPanel.add(btnPOS);
        navButtonsPanel.add(btnInventory);
        navButtonsPanel.add(btnMonitoring);

        // Change password section (only visible to admin)
        changePasswordSectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        changePasswordSectionPanel.setOpaque(false);
        changePasswordSectionPanel.setBorder(BorderFactory.createTitledBorder("Security"));
        changePasswordButton = new JButton("Change Admin Password");
        changePasswordButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        changePasswordButton.setMargin(new Insets(6, 10, 6, 10));
        changePasswordButton.setFocusable(false);
        changePasswordButton.setVisible(false);
        changePasswordButton.addActionListener(e -> openChangePasswordDialog());
        changePasswordSectionPanel.add(changePasswordButton);

        sidebar.add(topPanel, BorderLayout.NORTH);
        sidebar.add(navButtonsPanel, BorderLayout.CENTER);
        sidebar.add(changePasswordSectionPanel, BorderLayout.SOUTH);
    }

    /**
     * Styles a sidebar button with consistent appearance.
     */
    private void styleSidebarButton(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(new Color(220, 220, 220));
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMargin(new Insets(12, 20, 12, 20));
        button.setPreferredSize(new Dimension(200, 48));

        // Add hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(200, 200, 200));
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(220, 220, 220));
                }
            }
        });
    }

    /**
     * Adds a panel to the main display area.
     * 
     * @param panel The panel to add
     * @param name The name identifier for the panel
     */
    public void addPanel(JPanel panel, String name) {
        panelMap.put(name, panel);
        mainPanel.add(panel, name);
    }

    /**
     * Shows a specific panel by name.
     * Also refreshes the panel's data when shown.
     * 
     * @param name The name of the panel to show
     */
    public void showPanel(String name) {
        JPanel panel = panelMap.get(name);
        if (panel == null) {
            return;
        }

        // Refresh panel data when shown
        switch (name) {
            case "POS" -> posController.refreshPOS();
            case "Inventory" -> inventoryController.refreshInventory();
            case "Monitoring" -> monitoringController.refresh();
        }

        cardLayout.show(mainPanel, name);
    }

    /**
     * Handles switching between admin and cashier mode.
     * Requires password verification to switch to admin mode.
     */
    private void handleAdminToggle() {
        if (!isAdmin) {
            // Switch to admin mode - require password
            JPasswordField passwordField = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(this, passwordField, 
                    "Enter Admin Password:", JOptionPane.OK_CANCEL_OPTION);
            
            if (option == JOptionPane.OK_OPTION) {
                String password = new String(passwordField.getPassword());
                if (AdminDAO.verifyPassword(password)) {
                    isAdmin = true;
                    lblRole.setText("Admin");
                    JOptionPane.showMessageDialog(this, "Admin mode activated!");
                } else {
                    JOptionPane.showMessageDialog(this, "Incorrect password.", 
                            "Access Denied", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            // Switch back to cashier mode
            int confirm = JOptionPane.showConfirmDialog(this, 
                    "Switch back to Cashier mode?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                isAdmin = false;
                lblRole.setText("Cashier");
                JOptionPane.showMessageDialog(this, "Returned to Cashier mode.");
            }
        }
        updateAccess();
    }

    /**
     * Sets admin mode programmatically (called from login).
     * 
     * @param admin true to enable admin mode, false for cashier mode
     */
    public void setAdminMode(boolean admin) {
        this.isAdmin = admin;
        if (lblRole != null) {
            lblRole.setText(admin ? "Admin" : "Cashier");
        }
        updateAccess();
    }

    /**
     * Updates UI elements based on admin/cashier mode.
     * Enables or disables buttons and panels accordingly.
     */
    private void updateAccess() {
        // Enable/disable navigation buttons
        btnInventory.setEnabled(isAdmin);
        btnMonitoring.setEnabled(isAdmin);

        // Set tooltips
        btnPOS.setToolTipText("Point of Sale");
        btnInventory.setToolTipText(isAdmin ? "Inventory Management" : "Admin access required");
        btnMonitoring.setToolTipText(isAdmin ? "Sales Monitoring" : "Admin access required");

        // Update button colors
        Color disabledGray = new Color(200, 200, 200);
        btnInventory.setBackground(isAdmin ? new Color(220, 220, 220) : disabledGray);
        btnMonitoring.setBackground(isAdmin ? new Color(220, 220, 220) : disabledGray);

        // Enable/disable panels
        if (inventorySectionPanel != null) {
            setPanelEnabled(inventorySectionPanel, isAdmin);
        }
        if (monitoringSectionPanel != null) {
            setPanelEnabled(monitoringSectionPanel, isAdmin);
        }

        // Show/hide change password button
        if (changePasswordButton != null) {
            changePasswordButton.setVisible(isAdmin);
            changePasswordButton.setEnabled(isAdmin);
            changePasswordButton.setToolTipText(isAdmin ? 
                    "Change the admin password" : "Switch to Admin to change password");
        }
        if (changePasswordSectionPanel != null) {
            changePasswordSectionPanel.setVisible(isAdmin);
        }
    }

    /**
     * Enables or disables a panel and all its components.
     */
    private void setPanelEnabled(JPanel panel, boolean enabled) {
        panel.setEnabled(enabled);
        for (Component component : panel.getComponents()) {
            component.setEnabled(enabled);
        }
    }

    /**
     * Opens a dialog to change the admin password.
     * Only available in admin mode.
     */
    private void openChangePasswordDialog() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Only Admin can change the password.");
            return;
        }
        
        // Create password input fields
        JPasswordField passwordFieldCurrent = new JPasswordField();
        JPasswordField passwordFieldNew = new JPasswordField();
        JPasswordField passwordFieldConfirm = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Current Password:"));
        panel.add(passwordFieldCurrent);
        panel.add(new JLabel("New Password:"));
        panel.add(passwordFieldNew);
        panel.add(new JLabel("Confirm New Password:"));
        panel.add(passwordFieldConfirm);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Admin Password", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String currentPassword = new String(passwordFieldCurrent.getPassword());
        String newPassword = new String(passwordFieldNew.getPassword());
        String confirmPassword = new String(passwordFieldConfirm.getPassword());

        // Validate new password
        if (newPassword == null || newPassword.length() < 6) {
            JOptionPane.showMessageDialog(this, 
                    "New password must be at least 6 characters.", 
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, 
                    "New passwords do not match.", 
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Change the password
        boolean success = AdminDAO.changePassword(currentPassword, newPassword);
        if (success) {
            JOptionPane.showMessageDialog(this, "Admin password updated successfully.");
        } else {
            JOptionPane.showMessageDialog(this, 
                    "Current password is incorrect.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
