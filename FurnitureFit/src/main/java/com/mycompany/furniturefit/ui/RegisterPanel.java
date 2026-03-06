package com.mycompany.furniturefit.ui;

import com.mycompany.furniturefit.db.UserDAO;
import com.mycompany.furniturefit.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.border.AbstractBorder;

/**
 * Registration panel — 2-column form with placeholder text inside fields.
 * Matches the "Hello! Create your account" screenshot.
 */
public class RegisterPanel extends JPanel {

    private static final Color GREEN = new Color(45, 136, 45);
    private static final Color PLACEHOLDER_COLOR = new Color(0, 0, 0, 153);
    private static final Color INPUT_TEXT_COLOR = new Color(0, 0, 0);

    private final JTextField firstNameField;
    private final JTextField phoneField;
    private final JTextField lastNameField;
    private final JPasswordField passwordField;
    private final JTextField emailField;
    private final JPasswordField confirmPasswordField;
    private final JLabel statusLabel;
    private final BufferedImage backgroundImage;
    private final UserDAO userDAO;
    private Runnable onRegisterSuccess;
    private Runnable onSwitchToLogin;

    private final Font titleFont;
    private final Font subtitleFont;
    private final Font inputFont;
    private final Font buttonFont;

    public RegisterPanel() {
        titleFont = new Font("Segoe UI", Font.BOLD, 36);
        subtitleFont = new Font("Segoe UI", Font.BOLD, 24);
        inputFont = new Font("Segoe UI", Font.PLAIN, 18);
        buttonFont = new Font("Segoe UI", Font.BOLD, 18);

        userDAO = new UserDAO();
        backgroundImage = loadBackgroundImage();
        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
        setBackground(Color.BLACK);

        // Card — light grey rounded
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 40, gapy 16", "[grow, fill]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(0, 0, 0, 64));
                g2.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 20, 20);

                g2.setColor(new Color(217, 217, 217));
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(972, 570));

        // Title
        JLabel titleLabel = new JLabel("Hello!");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.BLACK);
        card.add(titleLabel, "left, gapbottom 0");

        JLabel subtitleLabel = new JLabel("Create your account");
        subtitleLabel.setFont(subtitleFont);
        subtitleLabel.setForeground(Color.BLACK);
        card.add(subtitleLabel, "left, gaptop 0.10, gapbottom 20");

        // 2-column grid — no labels, only placeholder fields
        JPanel formGrid = new JPanel(new MigLayout("wrap 2, insets 0, gapy 12, gapx 16",
                "[grow, fill][grow, fill]"));
        formGrid.setOpaque(false);

        // Row 1: First name | Phone (076)
        firstNameField = new JTextField();
        styleField(firstNameField);
        setupPlaceholder(firstNameField, "First name");
        formGrid.add(firstNameField, "growx, pushx, sg reginputs, h 52!");

        phoneField = new JTextField();
        styleField(phoneField);
        setupPlaceholder(phoneField, "Phone (076)");
        formGrid.add(phoneField, "growx, pushx, sg reginputs, h 52!");

        // Row 2: Last name | Password
        lastNameField = new JTextField();
        styleField(lastNameField);
        setupPlaceholder(lastNameField, "Last name");
        formGrid.add(lastNameField, "growx, pushx, sg reginputs, h 52!, gaptop 18");

        passwordField = new JPasswordField();
        styleField(passwordField);
        setupPasswordPlaceholder(passwordField, "Password");
        formGrid.add(passwordField, "growx, pushx, sg reginputs, h 52!, gaptop 18");

        // Row 3: Email | Confirm password
        emailField = new JTextField();
        styleField(emailField);
        setupPlaceholder(emailField, "Email");
        formGrid.add(emailField, "growx, pushx, sg reginputs, h 52!, gaptop 18");

        confirmPasswordField = new JPasswordField();
        styleField(confirmPasswordField);
        setupPasswordPlaceholder(confirmPasswordField, "Confirm password");
        formGrid.add(confirmPasswordField, "growx, pushx, sg reginputs, h 52!, gaptop 18");

        card.add(formGrid, "growx, pushx");

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(231, 76, 60));
        statusLabel.setFont(inputFont.deriveFont(12f));
        card.add(statusLabel, "center, gaptop 4");

        // Create button — green
        JButton createBtn = createPrimaryButton("Create");
        createBtn.addActionListener(e -> performRegistration());
        card.add(createBtn, "growx, pushx, h 52!");

        // "Already have an account?" link
        JButton loginLink = linkBtn("Already have an account?");
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g2.setColor(new Color(30, 32, 38));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        g2.dispose();
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
            clearFocusListeners(confirmPasswordField);
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
        field.setBackground(new Color(217, 217, 217));
        field.setForeground(PLACEHOLDER_COLOR);
        field.setCaretColor(INPUT_TEXT_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(0, 0, 0, 128), 1, 10),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        field.setFont(inputFont);
    }

    private void setupPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(PLACEHOLDER_COLOR);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(INPUT_TEXT_COLOR);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(PLACEHOLDER_COLOR);
                }
            }
        });
    }

    private void setupPasswordPlaceholder(JPasswordField field, String placeholder) {
        field.setEchoChar((char) 0);
        field.setText(placeholder);
        field.setForeground(PLACEHOLDER_COLOR);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(INPUT_TEXT_COLOR);
                    field.setEchoChar('\u2022');
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0);
                    field.setText(placeholder);
                    field.setForeground(PLACEHOLDER_COLOR);
                }
            }
        });
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setForeground(Color.WHITE);
        button.setFont(buttonFont);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton linkBtn(String text) {
        JButton b = new JButton("<html><u>" + text + "</u></html>");
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(new Color(80, 80, 80));
        b.setFont(inputFont.deriveFont(12f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(GREEN); }
            @Override public void mouseExited(MouseEvent e)  { b.setForeground(new Color(80, 80, 80)); }
        });
        return b;
    }

    private void clearFocusListeners(JTextField field) {
        for (FocusListener listener : field.getFocusListeners()) {
            field.removeFocusListener(listener);
        }
    }

    private BufferedImage loadBackgroundImage() {
        try {
            return ImageIO.read(getClass().getResource("/images/auth-bg.png"));
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    private static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = thickness;
            insets.right = thickness;
            insets.top = thickness;
            insets.bottom = thickness;
            return insets;
        }
    }

    public void setOnRegisterSuccess(Runnable callback) { this.onRegisterSuccess = callback; }
    public void setOnSwitchToLogin(Runnable callback)   { this.onSwitchToLogin = callback; }

    public void reset() {
        clearFocusListeners(firstNameField);
        clearFocusListeners(lastNameField);
        clearFocusListeners(emailField);
        clearFocusListeners(phoneField);
        clearFocusListeners(passwordField);
        clearFocusListeners(confirmPasswordField);

        setupPlaceholder(firstNameField, "First name");
        setupPlaceholder(lastNameField, "Last name");
        setupPlaceholder(emailField, "Email");
        setupPlaceholder(phoneField, "Phone (076)");
        setupPasswordPlaceholder(passwordField, "Password");
        setupPasswordPlaceholder(confirmPasswordField, "Confirm password");
        statusLabel.setText(" ");
    }
}
