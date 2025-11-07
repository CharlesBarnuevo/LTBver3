package LTBPaintCenter.model;

import javax.swing.*;
import java.awt.*;

public final class AdminAuthUtil {
    private AdminAuthUtil() {}

    public static boolean requireAdminPasswordPopup(Component parent) {
        JPasswordField pf = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(parent, pf, "Admin password required", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return false;
        String pass = new String(pf.getPassword());
        boolean ok = AdminDAO.verifyPassword(pass);
        if (!ok) {
            JOptionPane.showMessageDialog(parent, "Invalid admin password.", "Access denied", JOptionPane.WARNING_MESSAGE);
        }
        return ok;
    }
}
