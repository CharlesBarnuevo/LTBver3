package LTBPaintCenter.view;

import javax.swing.*;
import java.awt.*;

/**
 * This panel displays the login screen.
 * Users enter their username and password to access the system.
 */
public class LoginPanel extends JPanel {
    
    private final JTextField tfUser = new JTextField(12);
    private final JPasswordField pfPass = new JPasswordField(12);
    private final JButton btnLogin = new JButton("Login");

    /**
     * Constructor - creates the login form with username, password fields, and login button.
     */
    public LoginPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        
        // Username label and field
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        add(new JLabel("Username:"), constraints);
        
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        add(tfUser, constraints);

        // Password label and field
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.EAST;
        add(new JLabel("Password:"), constraints);
        
        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.WEST;
        add(pfPass, constraints);

        // Login button
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        add(btnLogin, constraints);
    }

    /**
     * Gets the username text field.
     * 
     * @return The username text field
     */
    public JTextField getTfUser() { 
        return tfUser; 
    }
    
    /**
     * Gets the password field.
     * 
     * @return The password field
     */
    public JPasswordField getPfPass() { 
        return pfPass; 
    }
    
    /**
     * Gets the login button.
     * 
     * @return The login button
     */
    public JButton getBtnLogin() { 
        return btnLogin; 
    }
}
