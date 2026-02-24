package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.model.Furniture;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Furniture picker shown as a floating panel on the left side of the editor.
 * Matches the "Furnish" floating panel in the screenshot:
 *   – "Furnish" title + X close
 *   – Favourites / Categories pill tabs
 *   – 2-column grid of rendered furniture thumbnails
 */
public class FurniturePickerDialog extends JDialog {

    private static final Color GREEN       = new Color(56, 124, 43);
    private static final Color CARD_BG     = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(210, 210, 210);

    private Furniture.Type selectedType;
    private boolean confirmed = false;

    public FurniturePickerDialog(Frame owner) {
        super(owner, true);   // modal so isConfirmed() is readable after setVisible
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(310, 480);
        // Position near the left sidebar (approx.)
        if (owner != null) {
            Point ownerLoc = owner.getLocationOnScreen();
            setLocation(ownerLoc.x + 14, ownerLoc.y + (owner.getHeight() - 480) / 2);
        }

        JPanel panel = buildPanel();
        setContentPane(panel);

        // Dismiss on Escape
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel buildPanel() {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 16, gapy 8", "[grow, fill]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18));
                g2.setColor(CARD_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // Header: "Furnish" title + X
        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]"));
        header.setOpaque(false);
        JLabel title = new JLabel("Furnish");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(40, 40, 40));
        header.add(title);
        JButton closeBtn = new JButton("\u2715");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeBtn.setForeground(new Color(100, 100, 100));
        closeBtn.setFocusPainted(false); closeBtn.setBorderPainted(false); closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        header.add(closeBtn);
        card.add(header, "growx");

        // Tabs: Favourites | Categories
        JPanel tabs = new JPanel(new MigLayout("insets 0, gap 6", "[][]"));
        tabs.setOpaque(false);
        JToggleButton favBtn = tabButton("Favourites", true);
        JToggleButton catBtn = tabButton("Categories", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(favBtn); bg.add(catBtn);
        tabs.add(favBtn, "h 28!");
        tabs.add(catBtn, "h 28!");
        card.add(tabs);

        // Furniture grid (2 columns of thumbnail buttons)
        JPanel grid = new JPanel(new MigLayout("wrap 2, gapy 8, gapx 8, insets 0",
                "[grow, fill][grow, fill]"));
        grid.setOpaque(false);

        ButtonGroup selGroup = new ButtonGroup();
        for (Furniture.Type type : Furniture.Type.values()) {
            JToggleButton btn = createThumbnailButton(type);
            selGroup.add(btn);
            grid.add(btn, "h 90!, grow");
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, "grow, push, h 300!");

        return card;
    }

    private JToggleButton tabButton(String text, boolean active) {
        JToggleButton btn = new JToggleButton(text);
        btn.setSelected(active);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        if (active) {
            btn.setBackground(GREEN);
            btn.setForeground(Color.WHITE);
            btn.setContentAreaFilled(true);
            btn.setOpaque(true);
        } else {
            btn.setBackground(new Color(230, 230, 230));
            btn.setForeground(new Color(80, 80, 80));
            btn.setContentAreaFilled(true);
            btn.setOpaque(true);
        }
        btn.addActionListener(e -> {
            boolean isActive = btn.isSelected();
            btn.setBackground(isActive ? GREEN : new Color(230, 230, 230));
            btn.setForeground(isActive ? Color.WHITE : new Color(80, 80, 80));
        });
        return btn;
    }

    private JToggleButton createThumbnailButton(Furniture.Type type) {
        JToggleButton btn = new JToggleButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background
                Color bg = isSelected() ? new Color(41, 180, 76, 30) : new Color(248, 248, 248);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(isSelected() ? GREEN : CARD_BORDER);
                g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                // Draw furniture icon centred in top 2/3
                drawFurnitureIcon(g2, type, getWidth(), (int)(getHeight() * 0.7));
                // Name label at bottom
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(60, 60, 60));
                FontMetrics fm = g2.getFontMetrics();
                String name = type.getDisplayName();
                g2.drawString(name, (getWidth() - fm.stringWidth(name)) / 2,
                        getHeight() - 6);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(0, 90));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            selectedType = type;
            confirmed = true;
            dispose();   // single-click selects and closes
        });
        return btn;
    }

    private void drawFurnitureIcon(Graphics2D g2, Furniture.Type type, int panelW, int iconH) {
        int cx = panelW / 2, cy = iconH / 2;
        int iw = (int)(panelW * 0.65), ih = (int)(iconH * 0.60);

        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case CHAIR -> {
                g2.setColor(new Color(101, 67, 33));
                g2.fillRoundRect(cx - iw / 4, cy - ih / 6, iw / 2, ih * 2 / 3, 4, 4);
                g2.fillRect(cx - iw / 4, cy - ih / 2, iw / 2, ih / 4);
                g2.setColor(new Color(70, 40, 10));
                g2.drawLine(cx - iw/4, cy + ih/2, cx - iw/4, cy + ih*3/4);
                g2.drawLine(cx + iw/4, cy + ih/2, cx + iw/4, cy + ih*3/4);
            }
            case DINING_TABLE, DESK -> {
                g2.setColor(new Color(160, 110, 60));
                g2.fillRoundRect(cx - iw / 2, cy - ih / 6, iw, ih / 3, 4, 4);
                g2.setColor(new Color(120, 80, 30));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(cx - iw / 2 + 6, cy + ih / 6, cx - iw / 2 + 6, cy + ih * 2 / 3);
                g2.drawLine(cx + iw / 2 - 6, cy + ih / 6, cx + iw / 2 - 6, cy + ih * 2 / 3);
            }
            case SOFA -> {
                g2.setColor(new Color(60, 90, 130));
                g2.fillRoundRect(cx - iw / 2, cy - ih / 3, iw, ih * 2 / 3, 6, 6);
                g2.setColor(new Color(40, 65, 100));
                g2.fillRect(cx - iw / 2, cy - ih / 3, iw, ih / 5);
                g2.fillRect(cx - iw / 2, cy - ih / 3, iw / 8, ih * 2 / 3);
                g2.fillRect(cx + iw * 3 / 8, cy - ih / 3, iw / 8, ih * 2 / 3);
            }
            case BED -> {
                g2.setColor(new Color(190, 190, 200));
                g2.fillRoundRect(cx - iw / 2, cy - ih / 3, iw, ih * 2 / 3, 4, 4);
                g2.setColor(new Color(220, 220, 230));
                g2.fillRoundRect(cx - iw / 3, cy - ih / 3 + 2, iw / 4, ih / 4, 4, 4);
                g2.fillRoundRect(cx + iw / 12, cy - ih / 3 + 2, iw / 4, ih / 4, 4, 4);
                g2.setColor(new Color(180, 180, 195));
                g2.fillRect(cx - iw / 2, cy - ih / 3, iw, ih / 8);
            }
            case SHELF, WARDROBE -> {
                g2.setColor(new Color(120, 80, 40));
                g2.fillRoundRect(cx - iw / 3, cy - ih / 2, iw * 2 / 3, ih, 4, 4);
                g2.setColor(new Color(95, 60, 25));
                for (int i = 1; i < 4; i++) {
                    g2.drawLine(cx - iw / 3 + 2, cy - ih / 2 + i * ih / 4,
                            cx + iw / 3 - 2, cy - ih / 2 + i * ih / 4);
                }
            }
            case SIDE_TABLE, COFFEE_TABLE -> {
                g2.setColor(new Color(180, 130, 80));
                g2.fillRoundRect(cx - iw / 3, cy - ih / 6, iw * 2 / 3, ih / 3, 4, 4);
                g2.setColor(new Color(140, 100, 55));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(cx - iw / 4, cy + ih / 6, cx - iw / 4, cy + ih / 2);
                g2.drawLine(cx + iw / 4, cy + ih / 6, cx + iw / 4, cy + ih / 2);
            }
            case LAMP -> {
                g2.setColor(new Color(220, 200, 160));
                g2.fillOval(cx - iw / 4, cy - ih / 2, iw / 2, ih / 3);
                g2.setColor(new Color(180, 160, 120));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(cx, cy - ih / 6, cx, cy + ih / 2);
                g2.fillOval(cx - 3, cy + ih / 2 - 3, 6, 6);
            }
        }
    }

    public boolean isConfirmed()      { return confirmed; }
    public Furniture.Type getSelectedType() { return selectedType; }
}
