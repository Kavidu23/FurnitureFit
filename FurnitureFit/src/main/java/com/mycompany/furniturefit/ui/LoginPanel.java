package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.db.UserDAO;
import com.mycompany.furnituredesignapp.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.border.AbstractBorder;

/**
 * Login panel — light card on full background image, green accent buttons.
 * Fields use placeholder text directly inside the field (no separate labels).
 */
public class LoginPanel extends JPanel {

    private static final Color GREEN = new Color(45, 136, 45);
    private static final Color PLACEHOLDER_COLOR = new Color(0, 0, 0, 153);
    private static final Color INPUT_TEXT_COLOR = new Color(0, 0, 0);
    private static final String PLACEHOLDER_EMAIL    = "Email";
    private static final String PLACEHOLDER_PASSWORD = "Password";

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel statusLabel;
    private final BufferedImage backgroundImage;
    private final UserDAO userDAO;
    private Runnable onLoginSuccess;
    private Runnable onSwitchToRegister;
    private User loggedInUser;

    private final Font titleFont;
    private final Font subtitleFont;
    private final Font inputFont;
    private final Font buttonFont;

    public LoginPanel() {
        // Use Segoe UI for everything
        titleFont = new Font("Segoe UI", Font.BOLD, 36);
        subtitleFont = new Font("Segoe UI", Font.BOLD, 24);
        inputFont = new Font("Segoe UI", Font.PLAIN, 18);
        buttonFont = new Font("Segoe UI", Font.BOLD, 18);

        userDAO = new UserDAO();
        backgroundImage = loadBackgroundImage();

        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
        setBackground(Color.BLACK);

        // Main card — light grey with rounded corners, shadow
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 40, gapy 16", "[grow, fill]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow
                g2.setColor(new Color(0, 0, 0, 64));
                g2.fillRoundRect(4, 4, getWidth() - 4, getHeight() - 4, 20, 20);

                // Background
                g2.setColor(new Color(217, 217, 217)); // #D9D9D9
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(506, 525));

        // "Hello!" title
        JLabel titleLabel = new JLabel("Hello!");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.BLACK);
        card.add(titleLabel, "left, gapbottom 0");

        // "Login to get started"
        JLabel subtitleLabel = new JLabel("Login to get started");
        subtitleLabel.setFont(subtitleFont);
        subtitleLabel.setForeground(Color.BLACK);
        card.add(subtitleLabel, "left, gaptop 0.10, gapbottom 32");

        // Email field
        usernameField = new JTextField();
        styleField(usernameField);
        setupPlaceholder(usernameField, PLACEHOLDER_EMAIL, false);
        card.add(usernameField, "growx, pushx, h 52!, gaptop 0");

        // Password field
        passwordField = new JPasswordField();
        styleField(passwordField);
        setupPasswordPlaceholder(passwordField, PLACEHOLDER_PASSWORD);
        card.add(passwordField, "growx, pushx, h 52!, gaptop 18");

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(231, 76, 60));
        statusLabel.setFont(inputFont.deriveFont(12f));
        card.add(statusLabel, "center, gaptop 4");


        // Login button
        JButton loginButton = createPrimaryButton("Login");
        loginButton.addActionListener(e -> performLogin());
        card.add(loginButton, "growx, pushx, h 52!");

        // Bottom row: Forgot Password | Create Account
        JPanel bottomRow = new JPanel(new MigLayout("insets 0", "[grow][]"));
        bottomRow.setOpaque(false);

        JButton forgotLink = linkBtn("Forgot Password?");
        forgotLink.addActionListener(e -> JOptionPane.showMessageDialog(
                this, "Please contact support to reset your password.", "Forgot Password", JOptionPane.INFORMATION_MESSAGE));

        JButton createLink = linkBtn("Create Account");
        createLink.setFont(subtitleFont.deriveFont(12f));
        createLink.addActionListener(e -> { if (onSwitchToRegister != null) onSwitchToRegister.run(); });

        bottomRow.add(forgotLink);
        bottomRow.add(createLink);
        card.add(bottomRow, "growx, pushx");

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
            clearFocusListeners(passwordField);
            setupPasswordPlaceholder(passwordField, PLACEHOLDER_PASSWORD);
        }
    }

    private void styleField(JTextField field) {
        field.setBackground(new Color(217, 217, 217)); // #D9D9D9
        field.setForeground(PLACEHOLDER_COLOR);
        field.setCaretColor(INPUT_TEXT_COLOR);

        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(new Color(0, 0, 0, 128), 1, 10),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        field.setFont(inputFont);
    }

    private void setupPlaceholder(JTextField field, String placeholder, boolean isPassword) {
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

    public void setOnLoginSuccess(Runnable callback)      { this.onLoginSuccess = callback; }
    public void setOnSwitchToRegister(Runnable callback)  { this.onSwitchToRegister = callback; }
    public User getLoggedInUser()                         { return loggedInUser; }

    public void reset() {
        usernameField.setText(PLACEHOLDER_EMAIL);
        usernameField.setForeground(PLACEHOLDER_COLOR);
        passwordField.setEchoChar((char) 0);
        passwordField.setText(PLACEHOLDER_PASSWORD);
        passwordField.setForeground(PLACEHOLDER_COLOR);
        statusLabel.setText(" ");
        loggedInUser = null;
        clearFocusListeners(passwordField);
        setupPasswordPlaceholder(passwordField, PLACEHOLDER_PASSWORD);
    }
}
