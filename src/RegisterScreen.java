
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.google.gson.JsonObject;
import java.io.IOException;

public class RegisterScreen extends JFrame {

    private JTextField usernameField, emailField;
    private JPasswordField passwordField;
    private JButton registerButton, backButton;
    private JComboBox<String> roleCombo;

    public RegisterScreen() {
        setTitle("Create Account");
        setSize(350, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Fields
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new JLabel("Email:"));
        emailField = new JTextField();
        panel.add(emailField);

        panel.add(new JLabel("Role:"));
        roleCombo = new JComboBox<>(new String[]{"customer", "manager"});
        panel.add(roleCombo);

        // Buttons
        registerButton = new JButton("Register");
        backButton = new JButton("Back");
        panel.add(registerButton);
        panel.add(backButton);

        add(panel);

        // Actions
        registerButton.addActionListener(e -> registerUser());
        backButton.addActionListener(e -> {
            this.dispose(); // close registration
            new LoginScreen().setVisible(true); // open login
        });
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String email = emailField.getText().trim();
        String userType = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("username", username);
            payload.addProperty("password", password);
            payload.addProperty("email", email);
            payload.addProperty("userType", userType);

            ApiService.register(payload); // call API method
            JOptionPane.showMessageDialog(this, "Registration successful! You can now login.");
            this.dispose();
            new LoginScreen().setVisible(true);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterScreen().setVisible(true));
    }
}


