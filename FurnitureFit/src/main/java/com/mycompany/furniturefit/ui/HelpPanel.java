package com.mycompany.furnituredesignapp.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

/**
 * Help panel with practical guidance matching the account panel design style.
 */
public class HelpPanel extends JPanel {

    private static final Color GREEN = new Color(45, 136, 45);
    private static final Color PANEL_BG = new Color(240, 242, 245);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color MUTED_TEXT = new Color(95, 95, 95);

    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 24);
    private final Font sectionTitleFont = new Font("Segoe UI", Font.BOLD, 18);
    private final Font bodyFont = new Font("Segoe UI", Font.PLAIN, 15);
    private final Font buttonFont = createButtonFont(15f);

    private Runnable onBack;

    public HelpPanel() {
        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
        setBackground(PANEL_BG);

        JPanel card = new JPanel(new MigLayout("fill, wrap 1, insets 16, gapy 10", "[469!, fill]", "[][grow, fill][]"));
        card.setBackground(CARD_BG);
        card.setBorder(new RoundedBorder(new Color(215, 215, 215), 1, 16));
        card.setPreferredSize(new Dimension(469, 540));

        JLabel title = new JLabel("Help & Guide", SwingConstants.CENTER);
        title.setFont(titleFont);
        title.setForeground(new Color(30, 30, 30));
        card.add(title, "center");

        JPanel content = new JPanel(new MigLayout("fillx, wrap 1, insets 0, gapy 8", "[grow, fill]"));
        content.setOpaque(false);

        content.add(createSectionCard(
                "Quick Start",
                "1. Create a new design from Dashboard.\n"
                        + "2. Set room dimensions first.\n"
                        + "3. Add furniture and arrange in 2D.\n"
                        + "4. Switch to 3D to review perspective.\n"
                        + "5. Save regularly."
        ), "growx");

        content.add(createSectionCard(
                "Editor Shortcuts",
                "Left Drag: Move selected furniture\n"
                        + "Right Click: Item actions\n"
                        + "Mouse Wheel: Zoom\n"
                        + "Alt + Drag: Pan\n"
                        + "R: Rotate selected item\n"
                        + "Delete: Remove selected item"
        ), "growx");

        content.add(createSectionCard(
                "Troubleshooting",
                "If a design does not appear, refresh from Dashboard.\n"
                        + "If movement feels slow, reduce object count in one room.\n"
                        + "If login fails, verify email and password exactly.\n"
                        + "For password reset, use account settings or support flow."
        ), "growx");

        content.add(createSectionCard(
                "Best Practice",
                "Keep at least 0.8m walking clearance.\n"
                        + "Group furniture by activity zone (sleep/work/relax).\n"
                        + "Use 3D preview before final save."
        ), "growx");

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scrollPane, "grow, push");

        JButton backBtn = createPrimaryButton("Back to Dashboard");
        backBtn.addActionListener(e -> {
            if (onBack != null) {
                onBack.run();
            }
        });
        card.add(backBtn, "growx, h 38!");

        add(card, "center");
    }

    private JPanel createSectionCard(String heading, String content) {
        JPanel panel = new JPanel(new MigLayout("fillx, wrap 1, insets 12, gapy 4", "[grow, fill]"));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new RoundedBorder(new Color(220, 220, 220), 1, 12));

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(sectionTitleFont);
        headingLabel.setForeground(new Color(35, 35, 35));
        panel.add(headingLabel);

        JTextArea body = new JTextArea(content);
        body.setEditable(false);
        body.setFocusable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setFont(bodyFont);
        body.setForeground(MUTED_TEXT);
        body.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        panel.add(body, "growx");

        return panel;
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

    private Font createButtonFont(float size) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.FAMILY, "Segoe UI");
        attrs.put(TextAttribute.SIZE, size);
        attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        return new Font(attrs);
    }

    public void setOnBack(Runnable callback) {
        this.onBack = callback;
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
