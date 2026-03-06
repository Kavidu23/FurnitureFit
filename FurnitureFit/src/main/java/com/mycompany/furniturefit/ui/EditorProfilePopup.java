package com.mycompany.furniturefit.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/** Profile popup menu shown from the editor toolbar avatar/name. */
public final class EditorProfilePopup {

    private static final Color GREEN = new Color(56, 124, 43);
    private static final Color RED = new Color(200, 55, 50);

    private EditorProfilePopup() {}

    public static void show(Component parent, String displayName, Point anchorScreenPos, Dimension anchorSize,
                            Runnable onDashboard, Runnable onLogout) {
        JDialog popup = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), false);
        popup.setUndecorated(true);
        popup.setBackground(new Color(0, 0, 0, 0));

        JPanel card = new JPanel(new MigLayout("wrap 1, insets 24 28 24 28, gapy 6", "[center, 220!]")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(220, 220, 220));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeBtn.setForeground(new Color(45, 45, 45));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> popup.dispose());

        JPanel topRow = new JPanel(new MigLayout("insets 0", "[]push[]"));
        topRow.setOpaque(false);
        topRow.add(new JLabel());
        topRow.add(closeBtn);
        card.add(topRow, "growx, gapbottom 6");

        JPanel avatarBig = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GREEN);
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawOval(2, 2, 76, 76);
                g2.setColor(new Color(220, 215, 235));
                g2.fillOval(6, 6, 68, 68);
                g2.setColor(new Color(128, 90, 180));
                g2.fillArc(18, 44, 44, 40, 0, 180);
                g2.setColor(new Color(240, 190, 140));
                g2.fillOval(26, 18, 28, 28);
                g2.dispose();
            }
        };
        avatarBig.setPreferredSize(new Dimension(80, 80));
        avatarBig.setOpaque(false);
        card.add(avatarBig, "center, gapbottom 6");

        JLabel nameLabel = new JLabel(displayName != null ? displayName : "User");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLabel.setForeground(new Color(40, 40, 40));
        card.add(nameLabel, "center, gapbottom 14");

        card.add(new JSeparator(), "growx, gapbottom 12");

        JButton dashboardBtn = actionButton("Go To Dashboard", GREEN);
        dashboardBtn.addActionListener(e -> {
            popup.dispose();
            onDashboard.run();
        });
        card.add(dashboardBtn, "growx, h 38!");

        JButton logoutBtn = actionButton("Log Out", RED);
        logoutBtn.addActionListener(e -> {
            popup.dispose();
            onLogout.run();
        });
        card.add(logoutBtn, "growx, h 38!");

        popup.setContentPane(card);
        popup.pack();

        if (anchorScreenPos != null && anchorSize != null) {
            popup.setLocation(anchorScreenPos.x - popup.getWidth() + anchorSize.width,
                    anchorScreenPos.y + anchorSize.height + 6);
        } else {
            popup.setLocationRelativeTo(parent);
        }

        popup.setVisible(true);
        popup.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override public void windowGainedFocus(java.awt.event.WindowEvent e) {}
            @Override public void windowLostFocus(java.awt.event.WindowEvent e) { popup.dispose(); }
        });
    }

    private static JButton actionButton(String labelText, Color bg) {
        JButton b = new JButton(labelText);
        b.setFont(new Font("Segoe UI", Font.BOLD, 15));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}

