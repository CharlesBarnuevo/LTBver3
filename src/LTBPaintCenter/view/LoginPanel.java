package LTBPaintCenter.view;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    private final JTextField tfUser = new JTextField(12);
    private final JPasswordField pfPass = new JPasswordField(12);
    private final JButton btnLogin = new JButton("Login");

    public LoginPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.EAST;
        add(new JLabel("Username:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        add(tfUser, c);

        c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.EAST;
        add(new JLabel("Password:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        add(pfPass, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        add(btnLogin, c);
    }

    public JTextField getTfUser() { return tfUser; }
    public JPasswordField getPfPass() { return pfPass; }
    public JButton getBtnLogin() { return btnLogin; }
}
