package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.db.UserDAO;
import com.mycompany.furnituredesignapp.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Login panel — light card on dark blurred background, green accent buttons.
 * Matches the "Hello! Login to get started" screenshot.
 * Fields use placeholder text directly inside the field (no separate labels).
 */
public class LoginPanel extends JPanel {

    private static final Color GREEN = new Color(56, 124, 43);
    private static final String PLACEHOLDER_EMAIL    = "Email";
    private static final String PLACEHOLDER_PASSWORD = "Password";

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel statusLabel;
    private final UserDAO userDAO;
    private Runnable onLoginSuccess;
    private Runnable onSwitchToRegister;
    private User loggedInUser;

    public LoginPanel() {
        userDAO = new UserDAO();
        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
        setBackground(new Color(30, 32, 38));

        // Main card — light grey with rounded corners
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 40 50 40 50, gapy 10", "[320!, center]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(238, 238, 238));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // "Hello!" title
        JLabel titleLabel = new JLabel("Hello!");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(30, 30, 30));
        card.add(titleLabel, "left, gapbottom 2");

        JLabel subtitleLabel = new JLabel("Login to get started");
        subtitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        subtitleLabel.setForeground(new Color(50, 50, 50));
        card.add(subtitleLabel, "left, gapbottom 20");

        // Email field with placeholder
        usernameField = new JTextField();
        styleField(usernameField);
        setupPlaceholder(usernameField, PLACEHOLDER_EMAIL, false);
        card.add(usernameField, "growx, h 44!");

        // Password field with placeholder
        passwordField = new JPasswordField();
        styleField(passwordField);
        setupPasswordPlaceholder(passwordField, PLACEHOLDER_PASSWORD);
        card.add(passwordField, "growx, h 44!, gaptop 12");

        // Status message
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(231, 76, 60));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        card.add(statusLabel, "center, gaptop 2");

        // Login button — green
        JButton loginButton = new JButton("Login");
        loginButton.setBackground(GREEN);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> performLogin());
        card.add(loginButton, "growx, h 46!, gaptop 8");

        // Bottom row: Forgot Password | Create Account
        JPanel bottomRow = new JPanel(new MigLayout("insets 0", "[grow][]"));
        bottomRow.setOpaque(false);

        JButton forgotLink = linkBtn("Forgot Password?");
        forgotLink.addActionListener(e -> JOptionPane.showMessageDialog(
                this, "Please contact support to reset your password.", "Forgot Password", JOptionPane.INFORMATION_MESSAGE));

        JButton createLink = linkBtn("Create Account");
        createLink.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createLink.addActionListener(e -> { if (onSwitchToRegister != null) onSwitchToRegister.run(); });

        bottomRow.add(forgotLink);
        bottomRow.add(createLink);
        card.add(bottomRow, "growx, gaptop 8");

        add(card, "center");

        // Enter key triggers login
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performLogin();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        if (username.equals(PLACEHOLDER_EMAIL)) username = "";
        char[] pw = passwordField.getPassword();
        String password = String.valueOf(pw);
        if (password.equals(PLACEHOLDER_PASSWORD)) password = "";

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both fields");
            return;
        }

        User user = userDAO.login(username, password);
        if (user != null) {
            loggedInUser = user;
            statusLabel.setForeground(GREEN);
            statusLabel.setText("Welcome, " + user.getFullName() + "!");
            if (onLoginSuccess != null) SwingUtilities.invokeLater(onLoginSuccess);
        } else {
            statusLabel.setForeground(new Color(231, 76, 60));
            statusLabel.setText("Invalid email or password");
            passwordField.setText("");
            setupPasswordPlaceholder(passwordField, PLACEHOLDER_PASSWORD);
        }
    }

    private void styleField(JTextField field) {
        field.setBackground(Color.WHITE);
        field.setForeground(new Color(130, 130, 130));
        field.setCaretColor(new Color(40, 40, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void setupPlaceholder(JTextField field, String placeholder, boolean isPassword) {
        field.setText(placeholder);
        field.setForeground(new Color(160, 160, 160));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(40, 40, 40));
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(160, 160, 160));
                }
            }
        });
    }

    private void setupPasswordPlaceholder(JPasswordField field, String placeholder) {
        field.setEchoChar((char) 0);
        field.setText(placeholder);
        field.setForeground(new Color(160, 160, 160));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(40, 40, 40));
                    field.setEchoChar('\u2022');
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0);
                    field.setText(placeholder);
                    field.setForeground(new Color(160, 160, 160));
                }
            }
        });
    }

    private JButton linkBtn(String text) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(new Color(80, 80, 80));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(GREEN); }
            @Override public void mouseExited(MouseEvent e)  { b.setForeground(new Color(80, 80, 80)); }
        });
        // underline via HTML is tricky in plain JButton; just use plain text
        return b;
    }

    public void setOnLoginSuccess(Runnable callback)      { this.onLoginSuccess = callback; }
    public void setOnSwitchToRegister(Runnable callback)  { this.onSwitchToRegister = callback; }
    public User getLoggedInUser()                         { return loggedInUser; }

    public void reset() {
        usernameField.setText(PLACEHOLDER_EMAIL);
        usernameField.setForeground(new Color(160, 160, 160));
        passwordField.setEchoChar((char) 0);
        passwordField.setText(PLACEHOLDER_PASSWORD);
        passwordField.setForeground(new Color(160, 160, 160));
        statusLabel.setText(" ");
        loggedInUser = null;
        // Re-attach focus listener for password field
        for (FocusListener fl : passwordField.getFocusListeners()) passwordField.removeFocusListener(fl);
        setupPasswordPlaceholder(passwordField, PLACEHOLDER_PASSWORD);
    }
}
