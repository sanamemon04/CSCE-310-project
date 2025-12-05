
import javax.swing.*;

import models.User;

// public class LoginScreen extends JFrame {
//     private JTextField usernameField;
//     private JPasswordField passwordField;
//     private JButton loginButton, registerButton;

//     public LoginScreen() {
//         setTitle("Bookstore Login");
//         setSize(300, 200);
//         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//         setLayout(null);

//         JLabel userLabel = new JLabel("Username:");
//         userLabel.setBounds(20, 20, 80, 25);
//         add(userLabel);

//         usernameField = new JTextField();
//         usernameField.setBounds(100, 20, 160, 25);
//         add(usernameField);

//         JLabel passLabel = new JLabel("Password:");
//         passLabel.setBounds(20, 60, 80, 25);
//         add(passLabel);

//         passwordField = new JPasswordField();
//         passwordField.setBounds(100, 60, 160, 25);
//         add(passwordField);

//         loginButton = new JButton("Login");
//         loginButton.setBounds(100, 100, 80, 25);
//         add(loginButton);

//                 registerButton = new JButton("Create New Account");


//         loginButton.addActionListener(e -> {
//             try {
//                 String username = usernameField.getText();
//                 String password = new String(passwordField.getPassword());
//                 User u = ApiService.login(username, password);
//                 System.out.println("TOKEN: " + u.getToken());
//                 JOptionPane.showMessageDialog(this, "Login successful!");

//                 dispose();
//                 if (u.getUserType().equals("customer")) {
//                     new CustomerScreen(u).setVisible(true);
//                 } else {
//                     new ManagerScreen(u).setVisible(true);
//                 }
//             } catch (Exception ex) {
//                 JOptionPane.showMessageDialog(this, "Login failed: " + ex.getMessage());
//             }
//         });
//     }
// }



import java.awt.*;

public class LoginScreen extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;

    public LoginScreen() {
        setTitle("Bookstore Login");
        setSize(400, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        registerButton = new JButton("Create New Account");
        loginButton = new JButton("Login");
        

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        
        panel.add(registerButton);
        panel.add(loginButton);

        add(panel);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> {
            new RegisterScreen().setVisible(true);
            dispose();
        });

        setVisible(true);
    }

    private void login() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try {
            User u = ApiService.login(username, password);

            if (u != null) {
                JOptionPane.showMessageDialog(this, "Login Successful!");
                dispose();
                if (u.getUserType().equals("customer")) {
                    new CustomerScreen(u).setVisible(true);
                } else {
                    new ManagerScreen(u).setVisible(true);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
