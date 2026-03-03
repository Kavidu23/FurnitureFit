package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.db.UserDAO;
import com.mycompany.furnituredesignapp.model.User;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Account panel for viewing and editing user profile.
 */
public class AccountPanel extends JPanel {

    private static final Color GREEN = new Color(45, 136, 45);
    private static final Color PANEL_BG = new Color(240, 242, 245);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color MUTED_TEXT = new Color(95, 95, 95);

    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 24);
    private final Font nameFont = new Font("Segoe UI", Font.BOLD, 18);
    private final Font bodyFont = new Font("Segoe UI", Font.PLAIN, 15);
    private final Font buttonFont = createMediumWeightFont(15f);

    private User currentUser;
    private final UserDAO userDAO;

    private final JLabel nameDisplay;
    private final JLabel usernameDisplay;
    private final JLabel joinedDisplay;
    private final AvatarPanel avatarPanel;

    private final BufferedImage defaultAvatarImage;

    private Runnable onBack;

    public AccountPanel() {
        userDAO = new UserDAO();
        defaultAvatarImage = loadDefaultAvatarImage();

        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
        setBackground(PANEL_BG);

        JPanel card = new JPanel(new MigLayout("fillx, wrap 1, insets 16, gapy 8", "[grow, fill]"));
        card.setBackground(CARD_BG);
        card.setBorder(new RoundedBorder(new Color(215, 215, 215), 1, 16));
        card.setPreferredSize(new Dimension(469, 490));

        JLabel title = new JLabel("My Account", SwingConstants.CENTER);
        title.setFont(titleFont);
        title.setForeground(new Color(30, 30, 30));
        card.add(title, "center, gapbottom 8");

        avatarPanel = new AvatarPanel();
        avatarPanel.setPreferredSize(new Dimension(122, 122));
        card.add(avatarPanel, "center");

        nameDisplay = new JLabel("-", SwingConstants.CENTER);
        nameDisplay.setFont(nameFont);
        nameDisplay.setForeground(new Color(25, 25, 25));
        card.add(nameDisplay, "center, gaptop 2");

        usernameDisplay = new JLabel("-", SwingConstants.CENTER);
        usernameDisplay.setFont(bodyFont);
        usernameDisplay.setForeground(MUTED_TEXT);
        card.add(usernameDisplay, "center");

        joinedDisplay = new JLabel("-", SwingConstants.CENTER);
        joinedDisplay.setFont(bodyFont);
        joinedDisplay.setForeground(new Color(120, 120, 120));
        card.add(joinedDisplay, "center, gapbottom 2");

        JPanel actions = new JPanel(new MigLayout("fillx, insets 0, wrap 1, gapy 6", "[grow, fill]"));
        actions.setOpaque(false);

        JButton changeNameBtn = createSecondaryButton("Change Display Name");
        changeNameBtn.addActionListener(e -> changeName());
        actions.add(changeNameBtn, "h 34!");

        JButton changePassBtn = createSecondaryButton("Change Password");
        changePassBtn.addActionListener(e -> changePassword());
        actions.add(changePassBtn, "h 34!");

        JButton backBtn = createPrimaryButton("Back to Dashboard");
        backBtn.addActionListener(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });
        actions.add(backBtn, "h 38!, gaptop 24");

        card.add(actions, "growx, pushy");

        add(card, "center");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;

        if (user != null) {
            nameDisplay.setText(user.getFullName() != null ? user.getFullName() : "-");
            usernameDisplay.setText(user.getUsername() != null ? user.getUsername() : "-");
            joinedDisplay.setText("Joined: " + (user.getCreatedAt() != null ? safeDate(user.getCreatedAt()) : "N/A"));
        } else {
            nameDisplay.setText("-");
            usernameDisplay.setText("-");
            joinedDisplay.setText("Joined: N/A");
        }

        avatarPanel.repaint();
        repaint();
    }

    private void changeName() {
        if (currentUser == null) {
            return;
        }

        String newName = JOptionPane.showInputDialog(this, "Enter new display name:", currentUser.getFullName());
        if (newName != null && !newName.trim().isEmpty()) {
            if (userDAO.updateFullName(currentUser.getId(), newName.trim())) {
                currentUser.setFullName(newName.trim());
                nameDisplay.setText(newName.trim());
                avatarPanel.repaint();
                JOptionPane.showMessageDialog(this, "Name updated successfully!");
            }
        }
    }

    private void changePassword() {
        if (currentUser == null) {
            return;
        }

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

        int result = JOptionPane.showConfirmDialog(this, panel, "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
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

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(buttonFont);
        button.setForeground(Color.WHITE);
        button.setBackground(GREEN);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        button.setForeground(new Color(45, 45, 45));
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new RoundedBorder(new Color(215, 215, 215), 1, 10));
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

      private Font createMediumWeightFont(float size) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.FAMILY, "Segoe UI");
        attrs.put(TextAttribute.SIZE, size);
        attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        return new Font(attrs);
    }

    private BufferedImage loadDefaultAvatarImage() {
        try {
            return ImageIO.read(getClass().getResource("/images/avatar.png"));
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    private String safeDate(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return "N/A";
        }
        return timestamp.length() >= 10 ? timestamp.substring(0, 10) : timestamp;
    }

    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }

    private class AvatarPanel extends JPanel {
        AvatarPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            Ellipse2D circle = new Ellipse2D.Double(x, y, size, size);

            g2.setColor(new Color(215, 230, 215));
            g2.fill(circle);
            g2.setColor(new Color(170, 200, 170));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(circle);

            Shape oldClip = g2.getClip();
            g2.setClip(circle);

            if (defaultAvatarImage != null) {
                g2.drawImage(defaultAvatarImage, x, y, size, size, null);
            } else {
                g2.setColor(new Color(180, 195, 215));
                g2.fillOval(x + size / 3, y + size / 5, size / 3, size / 3);
                g2.fillRoundRect(x + size / 4, y + size / 2, size / 2, size / 3, size / 3, size / 3);
            }

            g2.setClip(oldClip);
            g2.dispose();
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        RoundedBorder(Color color, int thickness, int radius) {
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
}
