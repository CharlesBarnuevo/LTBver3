package LTBPaintCenter.controller;

import LTBPaintCenter.model.AdminDAO;
import LTBPaintCenter.view.LoginPanel;
import LTBPaintCenter.model.Global;

import javax.swing.*;

public class LoginController {
    private final LoginPanel view;
    private final JFrame frame;

    public LoginController() {
        view = new LoginPanel();
        frame = new JFrame("LTB Paint Center — Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(view);
        frame.pack();
        frame.setLocationRelativeTo(null);

        attach();
    }

    private void attach() {
        view.getBtnLogin().addActionListener(e -> {
            String user = view.getTfUser().getText().trim();
            String pass = new String(view.getPfPass().getPassword());

            if (user.isBlank()) {
                JOptionPane.showMessageDialog(frame, "Enter username", "Login", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean authenticated = false;

            if ("admin".equalsIgnoreCase(user)) {
                // verify admin using DB hashed password
                authenticated = AdminDAO.verifyPassword(pass);
                if (!authenticated) {
                    JOptionPane.showMessageDialog(frame, "Invalid admin password", "Login failed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else if ("cashier".equalsIgnoreCase(user)) {
                // demo cashier password (hardcoded)
                if (!"cashier123".equals(pass)) {
                    JOptionPane.showMessageDialog(frame, "Invalid cashier password", "Login failed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                authenticated = true;
            } else {
                JOptionPane.showMessageDialog(frame, "Unknown username", "Login failed", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Success: set global state
            Global.currentUser = user;
            Global.isAdminMode = "admin".equalsIgnoreCase(user);

            // Launch main app on EDT
            SwingUtilities.invokeLater(() -> {
                frame.dispose();

                MainController mc = new MainController();

                if (Global.isAdminMode) {
                    try {
                        var frameObj = mc.getFrame(); // expects getFrame() to exist in MainController
                        if (frameObj != null) {
                            frameObj.setAdminMode(true);
                        }
                    } catch (NoSuchMethodError | NoClassDefFoundError | AbstractMethodError ex) {
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        });
    }

    public void show() {
        frame.setVisible(true);
    }
}
