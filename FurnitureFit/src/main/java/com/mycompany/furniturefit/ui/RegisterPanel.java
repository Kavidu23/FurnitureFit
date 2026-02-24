package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.db.UserDAO;
import com.mycompany.furnituredesignapp.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Registration panel — 2-column form with placeholder text inside fields.
 * Matches the "Hello! Create your account" screenshot.
 */
public class RegisterPanel extends JPanel {

    private static final Color GREEN = new Color(56, 124, 43);

    private final JTextField firstNameField;
    private final JTextField phoneField;
    private final JTextField lastNameField;
    private final JPasswordField passwordField;
    private final JTextField emailField;
    private final JPasswordField confirmPasswordField;
    private final JLabel statusLabel;
    private final UserDAO userDAO;
    private Runnable onRegisterSuccess;
    private Runnable onSwitchToLogin;

    public RegisterPanel() {
        userDAO = new UserDAO();
        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
        setBackground(new Color(30, 32, 38));

        // Card — light grey rounded
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 40 50 40 50, gapy 8", "[grow, fill]")) {
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

        // Title
        JLabel titleLabel = new JLabel("Hello!");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(30, 30, 30));
        card.add(titleLabel, "left, gapbottom 2");

        JLabel subtitleLabel = new JLabel("Create your account");
        subtitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        subtitleLabel.setForeground(new Color(50, 50, 50));
        card.add(subtitleLabel, "left, gapbottom 18");

        // 2-column grid — no labels, only placeholder fields
        JPanel formGrid = new JPanel(new MigLayout("wrap 2, insets 0, gapy 10, gapx 14",
                "[grow, fill][grow, fill]"));
        formGrid.setOpaque(false);

        // Row 1: First name | Phone (076)
        firstNameField = new JTextField();
        styleField(firstNameField);
        setupPlaceholder(firstNameField, "First name");
        formGrid.add(firstNameField, "h 42!");

        phoneField = new JTextField();
        styleField(phoneField);
        setupPlaceholder(phoneField, "Phone (076)");
        formGrid.add(phoneField, "h 42!");

        // Row 2: Last name | Password
        lastNameField = new JTextField();
        styleField(lastNameField);
        setupPlaceholder(lastNameField, "Last name");
        formGrid.add(lastNameField, "h 42!");

        passwordField = new JPasswordField();
        styleField(passwordField);
        setupPasswordPlaceholder(passwordField, "Password");
        formGrid.add(passwordField, "h 42!");

        // Row 3: Email | Confirm password
        emailField = new JTextField();
        styleField(emailField);
        setupPlaceholder(emailField, "Email");
        formGrid.add(emailField, "h 42!");

        confirmPasswordField = new JPasswordField();
        styleField(confirmPasswordField);
        setupPasswordPlaceholder(confirmPasswordField, "Confirm password");
        formGrid.add(confirmPasswordField, "h 42!");

        card.add(formGrid, "growx");

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(231, 76, 60));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        card.add(statusLabel, "center, gaptop 4");

        // Create button — green
        JButton createBtn = new JButton("Create");
        createBtn.setBackground(GREEN);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        createBtn.setFocusPainted(false);
        createBtn.setBorderPainted(false);
        createBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createBtn.addActionListener(e -> performRegistration());
        card.add(createBtn, "growx, h 46!, gaptop 8");

        // "Already have an account?" link
        JButton loginLink = new JButton("Already have an account?");
        loginLink.setBorderPainted(false);
        loginLink.setContentAreaFilled(false);
        loginLink.setForeground(new Color(80, 80, 80));
        loginLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLink.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { loginLink.setForeground(GREEN); }
            @Override public void mouseExited(MouseEvent e)  { loginLink.setForeground(new Color(80, 80, 80)); }
        });
        loginLink.addActionListener(e -> { if (onSwitchToLogin != null) onSwitchToLogin.run(); });
        card.add(loginLink, "center, gaptop 8");

        add(card, "center");

        // Enter key
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performRegistration();
            }
        };
        firstNameField.addKeyListener(enterKey);
        lastNameField.addKeyListener(enterKey);
        emailField.addKeyListener(enterKey);
        phoneField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);
        confirmPasswordField.addKeyListener(enterKey);
    }

    private void performRegistration() {
        String firstName = firstNameField.getText().trim();
        if (firstName.equals("First name")) firstName = "";
        String lastName = lastNameField.getText().trim();
        if (lastName.equals("Last name")) lastName = "";
        String email = emailField.getText().trim();
        if (email.equals("Email")) email = "";
        String password = String.valueOf(passwordField.getPassword());
        if (password.equals("Password")) password = "";
        String confirmPassword = String.valueOf(confirmPasswordField.getPassword());
        if (confirmPassword.equals("Confirm password")) confirmPassword = "";

        String fullName = (firstName + " " + lastName).trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }
        if (password.length() < 4) {
            showError("Password must be at least 4 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            confirmPasswordField.setText("");
            setupPasswordPlaceholder(confirmPasswordField, "Confirm password");
            return;
        }

        User user = userDAO.register(email, password, fullName);
        if (user != null) {
            statusLabel.setForeground(GREEN);
            statusLabel.setText("Account created! Please sign in.");
            if (onRegisterSuccess != null) {
                Timer timer = new Timer(1500, e -> onRegisterSuccess.run());
                timer.setRepeats(false);
                timer.start();
            }
        } else {
            showError("Email already registered. Try another.");
        }
    }

    private void showError(String message) {
        statusLabel.setForeground(new Color(231, 76, 60));
        statusLabel.setText(message);
    }

    private void styleField(JTextField field) {
        field.setBackground(Color.WHITE);
        field.setForeground(new Color(160, 160, 160));
        field.setCaretColor(new Color(40, 40, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void setupPlaceholder(JTextField field, String placeholder) {
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

    public void setOnRegisterSuccess(Runnable callback) { this.onRegisterSuccess = callback; }
    public void setOnSwitchToLogin(Runnable callback)   { this.onSwitchToLogin = callback; }

    public void reset() {
        setupPlaceholder(firstNameField, "First name");
        setupPlaceholder(lastNameField, "Last name");
        setupPlaceholder(emailField, "Email");
        setupPlaceholder(phoneField, "Phone (076)");
        for (FocusListener fl : passwordField.getFocusListeners())        passwordField.removeFocusListener(fl);
        for (FocusListener fl : confirmPasswordField.getFocusListeners()) confirmPasswordField.removeFocusListener(fl);
        setupPasswordPlaceholder(passwordField, "Password");
        setupPasswordPlaceholder(confirmPasswordField, "Confirm password");
        statusLabel.setText(" ");
    }
}

