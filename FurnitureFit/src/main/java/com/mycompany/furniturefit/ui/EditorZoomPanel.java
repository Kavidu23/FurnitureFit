package com.mycompany.furnituredesignapp.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/** Floating zoom panel (+/-) for the editor. */
public class EditorZoomPanel extends JPanel {

    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(210, 210, 210);

    public EditorZoomPanel(Runnable onZoomIn, Runnable onZoomOut) {
        super(new MigLayout("wrap 1, insets 4, gapy 2", "[center]"));
        setOpaque(false);
        setPreferredSize(new Dimension(45, 80));

        JButton plus = makeZoomBtn("+");
        plus.addActionListener(e -> onZoomIn.run());

        JButton minus = makeZoomBtn("-");
        minus.addActionListener(e -> onZoomOut.run());

        add(plus, "w 32!, h 32!");
        add(minus, "w 32!, h 32!");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(CARD_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        g2.setColor(CARD_BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.dispose();
    }

    private JButton makeZoomBtn(String label) {
        JButton b = new JButton(label);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        b.setForeground(new Color(100, 100, 100));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}

