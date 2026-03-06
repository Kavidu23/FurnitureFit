package com.mycompany.furniturefit.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/** Builds the floating help overlay used by the design editor. */
public final class EditorHelpOverlayFactory {

    private static final Color GREEN = new Color(56, 124, 43);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(210, 210, 210);

    private EditorHelpOverlayFactory() {}

    public static JPanel create(Runnable onClose) {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 14, gapy 8", "[grow, fill]")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]"));
        header.setOpaque(false);
        JLabel title = new JLabel("Help");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(40, 40, 40));
        header.add(title);

        JButton closeBtn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hov = getModel().isRollover();
                g2.setColor(hov ? new Color(200, 60, 60) : new Color(100, 100, 100));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2, sz = 5;
                g2.drawLine(cx - sz, cy - sz, cx + sz, cy + sz);
                g2.drawLine(cx + sz, cy - sz, cx - sz, cy + sz);
                g2.dispose();
            }
        };
        closeBtn.setRolloverEnabled(true);
        closeBtn.setPreferredSize(new Dimension(28, 28));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> onClose.run());
        header.add(closeBtn);
        card.add(header);

        JLabel subtitle = new JLabel("Shortcuts and controls");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(110, 110, 110));
        card.add(subtitle, "gapbottom 2");

        JPanel sections = new JPanel(new MigLayout("wrap 1, insets 0, gapy 8", "[grow, fill]"));
        sections.setOpaque(false);
        sections.add(sectionPanel("Keyboard",
                "Ctrl+Z: Undo\n" +
                "Ctrl+Y: Redo\n" +
                "Delete: Delete selected item\n" +
                "R: Rotate selected item (15 degrees)\n" +
                "Ctrl+0: Reset zoom and pan"));
        sections.add(sectionPanel("2D Controls",
                "Click: Select furniture\n" +
                "Drag: Move furniture\n" +
                "Right-click: Context menu\n" +
                "Scroll: Zoom in/out\n" +
                "Middle+Drag: Pan"));
        sections.add(sectionPanel("3D Controls",
                "Left drag: Orbit camera\n" +
                "Scroll: Zoom\n" +
                "Middle drag: Pan"));
        sections.add(sectionPanel("Quick Tips",
                "Add room first, then furniture and lights.\n" +
                "Use Save often to keep your latest changes.\n" +
                "Use Dashboard to reopen recent designs."));

        JScrollPane sp = new JScrollPane(sections);
        sp.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        sp.getVerticalScrollBar().setUnitIncrement(10);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        card.add(sp, "grow, push");

        return card;
    }

    private static JPanel sectionPanel(String title, String lines) {
        JPanel p = new JPanel(new MigLayout("wrap 1, insets 10, gapy 4", "[grow, fill]"));
        p.setOpaque(true);
        p.setBackground(new Color(248, 248, 248));
        p.setBorder(BorderFactory.createLineBorder(new Color(228, 228, 228)));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(new Color(45, 45, 45));
        p.add(t);

        JTextArea body = new JTextArea(lines);
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        body.setForeground(new Color(70, 70, 70));
        body.setOpaque(false);
        body.setBorder(null);
        p.add(body, "growx");

        return p;
    }
}

