package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.model.Room;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Room picker floating panel — matches the Furnish picker style.
 * Shows room shapes as 3D thumbnail previews.
 * Favourites: predefined room shapes; Categories: by size.
 */
public class RoomPickerDialog extends JDialog {

    private static final Color GREEN       = new Color(45, 136, 45);
    private static final Color CARD_BG     = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(210, 210, 210);

    /** Preset room configs shown in Favourites and Categories. */
    private static class RoomPreset {
        final String name;
        final double width, depth, height;
        final Room.Shape shape;

        RoomPreset(String name, double w, double d, double h, Room.Shape shape) {
            this.name = name; this.width = w; this.depth = d; this.height = h; this.shape = shape;
        }
    }

    // Single room preset
    private static final RoomPreset[] FAVOURITES = {
        new RoomPreset("Room", 4.0, 4.5, 2.8, Room.Shape.RECTANGULAR),
    };

    // Categories
    private static final String[] CAT_NAMES = { "Standard" };
    private static final RoomPreset[][] CAT_ITEMS = {
        {
            new RoomPreset("Room", 4.0, 4.5, 2.8, Room.Shape.RECTANGULAR),
        }
    };

    private RoomPreset selectedPreset;
    private boolean confirmed = false;
    private boolean showingFavourites = true;
    private JPanel gridContainer;
    private JToggleButton favBtn, catBtn;

    public RoomPickerDialog(Frame owner) {
        super(owner, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(320, 480);
        if (owner != null) {
            Point ownerLoc = owner.getLocationOnScreen();
            setLocation(ownerLoc.x + 14, ownerLoc.y + (owner.getHeight() - 480) / 2);
        }
        setContentPane(buildPanel());
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel buildPanel() {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 16, gapy 8", "[grow, fill]")) {
            @Override protected void paintComponent(Graphics g) {
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

        // Header
        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]"));
        header.setOpaque(false);
        JLabel title = new JLabel("Rooms");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(40, 40, 40));
        header.add(title);

        JButton closeBtn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hov = getModel().isRollover();
                g2.setColor(hov ? new Color(200, 60, 60) : new Color(100, 100, 100));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx2 = getWidth() / 2, cy2 = getHeight() / 2, sz = 5;
                g2.drawLine(cx2-sz, cy2-sz, cx2+sz, cy2+sz);
                g2.drawLine(cx2+sz, cy2-sz, cx2-sz, cy2+sz);
                g2.dispose();
            }
        };
        closeBtn.setRolloverEnabled(true);
        closeBtn.setPreferredSize(new Dimension(28, 28));
        closeBtn.setFocusPainted(false); closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false); closeBtn.setOpaque(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        header.add(closeBtn);
        card.add(header, "growx");

        // Tabs
        JPanel tabs = new JPanel(new MigLayout("insets 0, gap 6", "[grow, fill][grow, fill]"));
        tabs.setOpaque(false);
        favBtn = tabButton("Favourites", true);
        catBtn = tabButton("Categories", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(favBtn); bg.add(catBtn);
        favBtn.addActionListener(e -> { showingFavourites = true; updateTabStyles(); showFavourites(); });
        catBtn.addActionListener(e -> { showingFavourites = false; updateTabStyles(); showCategories(); });
        tabs.add(favBtn, "growx, h 30!");
        tabs.add(catBtn, "growx, h 30!");
        card.add(tabs);

        // Grid
        gridContainer = new JPanel(new MigLayout("wrap 1, insets 0", "[grow, fill]"));
        gridContainer.setOpaque(false);
        showFavourites();

        JScrollPane scroll = new JScrollPane(gridContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, "grow, push, h 340!");

        return card;
    }

    // ── Favourites view ──
    private void showFavourites() {
        gridContainer.removeAll();
        JPanel grid = new JPanel(new MigLayout("wrap 1, insets 0", "[grow, fill]"));
        grid.setOpaque(false);
        for (RoomPreset p : FAVOURITES) {
            grid.add(createThumbnailButton(p), "h 100!");
        }
        gridContainer.add(grid);
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    // ── Categories view ──
    private void showCategories() {
        gridContainer.removeAll();
        for (int i = 0; i < CAT_NAMES.length; i++) {
            JLabel catLabel = new JLabel(CAT_NAMES[i]);
            catLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            catLabel.setForeground(new Color(80, 80, 80));
            gridContainer.add(catLabel, "gaptop 6");
            JPanel grid = new JPanel(new MigLayout("wrap 1, insets 0", "[grow, fill]"));
            grid.setOpaque(false);
            for (RoomPreset p : CAT_ITEMS[i]) {
                grid.add(createThumbnailButton(p), "h 100!");
            }
            gridContainer.add(grid);
        }
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private void updateTabStyles() {
        styleTab(favBtn, showingFavourites);
        styleTab(catBtn, !showingFavourites);
    }

    private JToggleButton tabButton(String text, boolean active) {
        JToggleButton b = new JToggleButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? GREEN : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        b.setSelected(active);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        styleTab(b, active);
        return b;
    }

    private void styleTab(JToggleButton b, boolean active) {
        b.setForeground(active ? Color.WHITE : Color.BLACK);
        b.repaint();
    }

    // ── Thumbnail button for a room preset ──
    private JButton createThumbnailButton(RoomPreset preset) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hov = getModel().isRollover();

                // Card background
                g2.setColor(hov ? new Color(245, 248, 245) : new Color(250, 250, 250));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(hov ? GREEN : CARD_BORDER);
                g2.setStroke(new BasicStroke(hov ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                // Draw room preview
                int pw = getWidth(), ph = getHeight();
                drawRoomPreview(g2, preset, pw, ph - 20);

                // Name label at bottom
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(80, 80, 80));
                FontMetrics fm = g2.getFontMetrics();
                String label = preset.name;
                g2.drawString(label, (pw - fm.stringWidth(label)) / 2, ph - 6);

                g2.dispose();
            }
        };
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            selectedPreset = preset;
            confirmed = true;
            dispose();
        });
        return b;
    }

    /**
     * Draws a simple isometric 3D room preview inside the thumbnail.
     */
    private void drawRoomPreview(Graphics2D g2, RoomPreset preset, int pw, int ph) {
        int rw = (int) (pw * 0.58);
        int rh = (int) (ph * 0.42);
        int rx = (pw - rw) / 2;
        int ry = (ph - rh) / 2 + 4;

        g2.setColor(new Color(230, 225, 215));
        g2.fillRoundRect(rx, ry, rw, rh, 8, 8);
        g2.setColor(new Color(120, 120, 120));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(rx, ry, rw, rh, 8, 8);
    }

    // Public API ──
    public boolean isConfirmed() { return confirmed; }

    public void applyToRoom(Room room) {
        if (selectedPreset != null) {
            room.setWidth(selectedPreset.width);
            room.setDepth(selectedPreset.depth);
            room.setHeight(selectedPreset.height);
            room.setShape(selectedPreset.shape);
        }
    }
}

