package LTBPaintCenter.controller;

import LTBPaintCenter.model.AdminDAO;
import LTBPaintCenter.view.LoginPanel;
import LTBPaintCenter.controller.MainController;
import LTBPaintCenter.model.Global;

import javax.swing.*;

public class LoginController {
    private final LoginPanel view;
    private final JFrame frame;

    public LoginController() {
        view = new LoginPanel();
        frame = new JFrame("Login");
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

            if ("admin".equalsIgnoreCase(user)) {
                boolean ok = AdminDAO.verifyPassword(pass);
                if (!ok) {
                    JOptionPane.showMessageDialog(frame, "Invalid admin password", "Login failed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else if ("cashier".equalsIgnoreCase(user)) {
                if (!"cashier123".equals(pass)) {
                    JOptionPane.showMessageDialog(frame, "Invalid cashier password", "Login failed", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Unknown username", "Login failed", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Global.currentUser = user;

            SwingUtilities.invokeLater(() -> {
                frame.dispose();
                new MainController();
            });
        });
    }

    public void show() {
        frame.setVisible(true);
    }
}
