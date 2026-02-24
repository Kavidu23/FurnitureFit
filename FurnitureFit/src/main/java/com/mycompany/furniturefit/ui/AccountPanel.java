package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.db.UserDAO;
import com.mycompany.furnituredesignapp.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Account panel for viewing and editing user profile.
 */
public class AccountPanel extends JPanel {

    private User currentUser;
    private final UserDAO userDAO;
    private final JLabel nameDisplay;
    private final JLabel usernameDisplay;
    private final JLabel joinedDisplay;
    private Runnable onBack;

    public AccountPanel() {
        userDAO = new UserDAO();
        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
        setBackground(new Color(245, 245, 245));

        JPanel card = new JPanel(new MigLayout("wrap 1, insets 30 40 30 40, gapy 8", "[350!, center]"));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210), 1));

        // Title
        JLabel title = new JLabel("My Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(56, 124, 43));
        card.add(title, "center, gapbottom 15");

        // Avatar placeholder
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(56, 124, 43));
                g2d.fillOval(10, 5, 60, 60);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
                String initial = currentUser != null ? currentUser.getFullName().substring(0, 1).toUpperCase() : "?";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(initial, 40 - fm.stringWidth(initial) / 2, 42);
            }
        };
        avatar.setPreferredSize(new Dimension(80, 70));
        avatar.setOpaque(false);
        card.add(avatar, "center, gapbottom 10");

        // Info
        JPanel infoPanel = new JPanel(new MigLayout("wrap 2, insets 0, gapy 8", "[right, 100!][grow, fill]"));
        infoPanel.setOpaque(false);

        infoPanel.add(createLabel("Full Name:"));
        nameDisplay = new JLabel("-");
        nameDisplay.setForeground(new Color(40, 40, 40));
        nameDisplay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        infoPanel.add(nameDisplay);

        infoPanel.add(createLabel("Username:"));
        usernameDisplay = new JLabel("-");
        usernameDisplay.setForeground(new Color(80, 80, 80));
        usernameDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoPanel.add(usernameDisplay);

        infoPanel.add(createLabel("Joined:"));
        joinedDisplay = new JLabel("-");
        joinedDisplay.setForeground(new Color(100, 100, 100));
        joinedDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoPanel.add(joinedDisplay);

        card.add(infoPanel);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(210, 210, 210));
        card.add(sep, "growx, gaptop 10, gapbottom 10");

        // Change name
        JButton changeNameBtn = new JButton("Change Display Name");
        changeNameBtn.setFocusPainted(false);
        changeNameBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        changeNameBtn.addActionListener(e -> changeName());
        card.add(changeNameBtn, "growx, h 32!");

        // Change password
        JButton changePassBtn = new JButton("Change Password");
        changePassBtn.setFocusPainted(false);
        changePassBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        changePassBtn.addActionListener(e -> changePassword());
        card.add(changePassBtn, "growx, h 32!");

        // Back button
        JButton backBtn = new JButton("← Back to Dashboard");
        backBtn.setBackground(new Color(56, 124, 43));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> { if (onBack != null) onBack.run(); });
        card.add(backBtn, "growx, h 36!, gaptop 15");

        add(card, "center");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            nameDisplay.setText(user.getFullName());
            usernameDisplay.setText(user.getUsername());
            joinedDisplay.setText(user.getCreatedAt() != null ? user.getCreatedAt().substring(0, 10) : "N/A");
        }
        repaint();
    }

    private void changeName() {
        if (currentUser == null) return;
        String newName = JOptionPane.showInputDialog(this, "Enter new display name:",
                currentUser.getFullName());
        if (newName != null && !newName.trim().isEmpty()) {
            if (userDAO.updateFullName(currentUser.getId(), newName.trim())) {
                currentUser.setFullName(newName.trim());
                nameDisplay.setText(newName.trim());
                JOptionPane.showMessageDialog(this, "Name updated successfully!");
            }
        }
    }

    private void changePassword() {
        if (currentUser == null) return;
        JPanel panel = new JPanel(new MigLayout("wrap 2", "[right][grow, fill]"));
        JPasswordField oldPass = new JPasswordField(15);
        JPasswordField newPass = new JPasswordField(15);
        JPasswordField confirmPass = new JPasswordField(15);
        panel.add(new JLabel("Current Password:"));
        panel.add(oldPass);
        panel.add(new JLabel("New Password:"));
        panel.add(newPass);
        panel.add(new JLabel("Confirm:"));
        panel.add(confirmPass);

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String oldP = new String(oldPass.getPassword());
            String newP = new String(newPass.getPassword());
            String confirmP = new String(confirmPass.getPassword());

            if (newP.isEmpty() || newP.length() < 4) {
                JOptionPane.showMessageDialog(this, "Password must be at least 4 characters.");
                return;
            }
            if (!newP.equals(confirmP)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.");
                return;
            }
            if (userDAO.updatePassword(currentUser.getId(), oldP, newP)) {
                JOptionPane.showMessageDialog(this, "Password changed successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Current password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(140, 145, 155));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }

    public void setOnBack(Runnable callback) { this.onBack = callback; }
}
