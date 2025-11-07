package LTBPaintCenter;

import javax.swing.SwingUtilities;
import LTBPaintCenter.controller.LoginController;
import LTBPaintCenter.model.DatabaseSetup;

public class App {
    public static void main(String[] args) {
        DatabaseSetup.initializeDatabase();

        SwingUtilities.invokeLater(() -> {
            new LoginController().show();
        });
    }
}
