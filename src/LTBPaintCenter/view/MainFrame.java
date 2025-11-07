package LTBPaintCenter.view;

import LTBPaintCenter.controller.*;
import LTBPaintCenter.dao.AdminDAO;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

    //Main application frame with sidebar navigation, admin toggle, and module panel management.
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

    public MainFrame(POSController posCtrl, InventoryController invCtrl, MonitoringController monCtrl) {
        this.posController = posCtrl;
        this.inventoryController = invCtrl;
        this.monitoringController = monCtrl;

        setTitle("Product Management System for LTB Paint Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        initSidebar();
        add(sidebar, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210,210,210)));
        statusBar.setBackground(Color.WHITE);

        JPanel leftStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        leftStatus.setOpaque(false);
        JLabel lblDateTime = new JLabel();
        lblDateTime.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        leftStatus.add(lblDateTime);
        statusBar.add(leftStatus, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            lblDateTime.setText("System Date/Time: " + now);
        });
        timer.setRepeats(true);
        timer.start();

        addPanel(posController.getView(), "POS");
        addPanel(inventoryController.getView(), "Inventory");
        addPanel(monitoringController.getView(), "Monitoring");

        btnPOS.addActionListener(e -> showPanel("POS"));
        btnInventory.addActionListener(e -> showPanel("Inventory"));
        btnMonitoring.addActionListener(e -> showPanel("Monitoring"));

        showPanel("POS");
        updateAccess();
    }

    private void initSidebar() {
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBackground(new Color(240, 240, 240));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(210, 210, 210)));

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

        navPanel.setBackground(new Color(240, 240, 240));
        styleSidebarButton(btnPOS);
        styleSidebarButton(btnInventory);
        styleSidebarButton(btnMonitoring);

        JPanel navButtonsPanel = new JPanel();
        navButtonsPanel.setOpaque(false);
        navButtonsPanel.setLayout(new GridLayout(3, 1, 10, 10));
        navButtonsPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        navButtonsPanel.add(btnPOS);
        navButtonsPanel.add(btnInventory);
        navButtonsPanel.add(btnMonitoring);

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

    private void styleSidebarButton(JButton b) {
        b.setFocusPainted(false);
        b.setBackground(new Color(220, 220, 220));
        b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMargin(new Insets(12, 20, 12, 20));
        b.setPreferredSize(new Dimension(200, 48));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(new Color(200, 200, 200));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(new Color(220, 220, 220));
            }
        });
    }

    public void addPanel(JPanel panel, String name) {
        panelMap.put(name, panel);
        mainPanel.add(panel, name);
    }

    public void showPanel(String name) {
        JPanel panel = panelMap.get(name);
        if (panel == null) return;

        switch (name) {
            case "POS" -> posController.refreshPOS();
            case "Inventory" -> inventoryController.refreshInventory();
            case "Monitoring" -> monitoringController.refresh();
        }

        cardLayout.show(mainPanel, name);
    }

    private void handleAdminToggle() {
        if (!isAdmin) {
            JPasswordField pf = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(this, pf, "Enter Admin Password:", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String pass = new String(pf.getPassword());
                if (AdminDAO.verifyPassword(pass)) {
                    isAdmin = true;
                    lblRole.setText("Admin");
                    JOptionPane.showMessageDialog(this, "Admin mode activated!");
                } else {
                    JOptionPane.showMessageDialog(this, "Incorrect password.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(this, "Switch back to Cashier mode?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                isAdmin = false;
                lblRole.setText("Cashier");
                JOptionPane.showMessageDialog(this, "Returned to Cashier mode.");
            }
        }
        updateAccess();
    }

    public void setAdminMode(boolean admin) {
        this.isAdmin = admin;
        if (lblRole != null) lblRole.setText(admin ? "Admin" : "Cashier");
        updateAccess();
    }

    private void updateAccess() {
        btnInventory.setEnabled(isAdmin);
        btnMonitoring.setEnabled(isAdmin);

        btnPOS.setToolTipText("Point of Sale");
        btnInventory.setToolTipText(isAdmin ? "Inventory Management" : "Admin access required");
        btnMonitoring.setToolTipText(isAdmin ? "Sales Monitoring" : "Admin access required");

        Color disabledGray = new Color(200, 200, 200);
        btnInventory.setBackground(isAdmin ? new Color(220, 220, 220) : disabledGray);
        btnMonitoring.setBackground(isAdmin ? new Color(220, 220, 220) : disabledGray);

        if (inventorySectionPanel != null) setPanelEnabled(inventorySectionPanel, isAdmin);
        if (monitoringSectionPanel != null) setPanelEnabled(monitoringSectionPanel, isAdmin);

        if (changePasswordButton != null) {
            changePasswordButton.setVisible(isAdmin);
            changePasswordButton.setEnabled(isAdmin);
            changePasswordButton.setToolTipText(isAdmin ? "Change the admin password" : "Switch to Admin to change password");
        }
        if (changePasswordSectionPanel != null) {
            changePasswordSectionPanel.setVisible(isAdmin);
        }
    }

    private void setPanelEnabled(JPanel panel, boolean enabled) {
        panel.setEnabled(enabled);
        for (Component c : panel.getComponents()) {
            c.setEnabled(enabled);
        }
    }

    private void openChangePasswordDialog() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(this, "Only Admin can change the password.");
            return;
        }
        JPasswordField pfCurrent = new JPasswordField();
        JPasswordField pfNew = new JPasswordField();
        JPasswordField pfConfirm = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Current Password:"));
        panel.add(pfCurrent);
        panel.add(new JLabel("New Password:"));
        panel.add(pfNew);
        panel.add(new JLabel("Confirm New Password:"));
        panel.add(pfConfirm);

        int res = JOptionPane.showConfirmDialog(this, panel, "Change Admin Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String current = new String(pfCurrent.getPassword());
        String np = new String(pfNew.getPassword());
        String cf = new String(pfConfirm.getPassword());

        if (np == null || np.length() < 6) {
            JOptionPane.showMessageDialog(this, "New password must be at least 6 characters.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!np.equals(cf)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean ok = AdminDAO.changePassword(current, np);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Admin password updated successfully.");
        } else {
            JOptionPane.showMessageDialog(this, "Current password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}