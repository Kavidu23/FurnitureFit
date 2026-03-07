package com.mycompany.furniturefit.ui;

import com.mycompany.furniturefit.model.Furniture;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Enhanced Lights panel — adds light fixtures as 3D objects to the room.
 * 
 * Features:
 *   – Select a light type to ADD it to the room as a 3D object
 *   – Pick a light color (warm white, cool white, golden, etc.)
 *   – Adjust brightness and angle
 *   – Favourites / Categories tabs
 *   – Apply settings or Add light to scene
 */
public class LightingDialog extends JDialog {

    private static final Color GREEN       = new Color(45, 136, 45);
    private static final Color CARD_BG     = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(210, 210, 210);

    private final JSlider intensitySlider;
    private final JSlider directionSlider;
    private boolean confirmed = false;
    private boolean addLightToScene = false;
    private double intensity = 0.3;
    private double direction = 45;
    private Furniture.Type selectedLightType = null;
    private Color selectedLightColor = new Color(255, 240, 200); // warm white

    // Light types mapped to furniture types (Pendant and Wall removed)
    private static final Object[][] LIGHT_ITEMS = {
        { "Floor Lamp", Furniture.Type.FLOOR_LAMP_LIGHT },
        { "Ceiling",    Furniture.Type.CEILING_LIGHT },
        { "Spotlight",  Furniture.Type.SPOTLIGHT },
        { "Table Lamp", Furniture.Type.TABLE_LAMP_LIGHT },
    };

    // Favourites
    private static final int[] FAVOURITES = { 1, 0, 2, 3 };

    // Categories
    private static final String[] CATEGORY_NAMES = { "Ambient", "Task", "Accent" };
    private static final int[][] CATEGORY_ITEMS = {
        { 1, 2 },    // Ambient: Ceiling, Spotlight
        { 3, 0 },    // Task: Table Lamp, Floor Lamp
        { 2, 3 },    // Accent: Spotlight, Table Lamp
    };

    // Preset light colors
    private static final Color[] LIGHT_COLORS = {
        new Color(255, 240, 200),  // Warm White
        new Color(255, 255, 245),  // Cool White
        new Color(255, 210, 100),  // Golden
        new Color(255, 180, 80),   // Amber
        new Color(200, 220, 255),  // Daylight Blue
        new Color(255, 160, 120),  // Sunset
        new Color(180, 255, 180),  // Soft Green
        new Color(220, 180, 255),  // Lavender
    };
    private static final String[] LIGHT_COLOR_NAMES = {
        "Warm White", "Cool White", "Golden", "Amber",
        "Daylight", "Sunset", "Soft Green", "Lavender"
    };

    private JPanel gridContainer;
    private JToggleButton favBtn, catBtn;
    private boolean showingFavourites = true;
    private ButtonGroup lightSelGroup; // shared across tabs

    public LightingDialog(Frame owner) {
        super(owner, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(320, 650);
        if (owner != null) {
            Point loc = owner.getLocationOnScreen();
            setLocation(loc.x + 14, loc.y + (owner.getHeight() - 620) / 2);
        }

        intensitySlider = new JSlider(0, 100, 30);
        directionSlider = new JSlider(0, 360, 45);
        lightSelGroup = new ButtonGroup();
        configureSliders();

        setContentPane(buildPanel());
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel buildPanel() {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 16, gapy 6", "[grow, fill]")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18));
                g2.setColor(CARD_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth()-1, getHeight()-1, 18, 18));
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // ── Header ──
        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]"));
        header.setOpaque(false);
        JLabel title = new JLabel("Lights");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(40, 40, 40));
        header.add(title);

        JButton close = makeCloseButton();
        header.add(close);
        card.add(header, "growx");

        // ── Tabs ──
        JPanel tabs = new JPanel(new MigLayout("insets 0, gap 6", "[grow, fill][grow, fill]"));
        tabs.setOpaque(false);
        favBtn = tabButton("Favourites", true);
        catBtn = tabButton("Categories", false);
        ButtonGroup tg = new ButtonGroup();
        tg.add(favBtn); tg.add(catBtn);

        favBtn.addActionListener(e -> { showingFavourites = true;  updateTabStyles(); showFavourites(); });
        catBtn.addActionListener(e -> { showingFavourites = false; updateTabStyles(); showCategories(); });

        tabs.add(favBtn, "growx, h 30!");
        tabs.add(catBtn, "growx, h 30!");
        card.add(tabs);

        // ── Light thumbnails grid ──
        gridContainer = new JPanel(new MigLayout("wrap 1, insets 0", "[grow, fill]"));
        gridContainer.setOpaque(false);
        showFavourites();

        JScrollPane scroll = new JScrollPane(gridContainer);
        scroll.setBorder(null); scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, "grow, push, h 175!, gaptop 8, gapbottom 12");

        // ── Light Color Picker ──
        card.add(new JSeparator(), "growx, gaptop 4, gapbottom 2");
        JLabel colorTitle = new JLabel("Light Color");
        colorTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        colorTitle.setForeground(new Color(50, 50, 50));
        card.add(colorTitle);

        JPanel colorGrid = new JPanel(new MigLayout("wrap 4, insets 2, gap 6", "[][][][]"));
        colorGrid.setOpaque(false);
        ButtonGroup colorGroup = new ButtonGroup();
        for (int i = 0; i < LIGHT_COLORS.length; i++) {
            final Color lc = LIGHT_COLORS[i];
            final String lcName = LIGHT_COLOR_NAMES[i];
            JToggleButton cb = new JToggleButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean sel = isSelected();
                    boolean hov = getModel().isRollover();
                    // Glow circle
                    if (sel || hov) {
                        g2.setColor(new Color(lc.getRed(), lc.getGreen(), lc.getBlue(), 80));
                        g2.fillOval(0, 0, getWidth(), getHeight());
                    }
                    // Main color circle
                    int inset = sel ? 2 : 4;
                    g2.setColor(lc);
                    g2.fillOval(inset, inset, getWidth() - inset * 2, getHeight() - inset * 2);
                    // Border
                    g2.setColor(sel ? GREEN : new Color(180, 180, 180));
                    g2.setStroke(new BasicStroke(sel ? 2.5f : 1f));
                    g2.drawOval(inset, inset, getWidth() - inset * 2 - 1, getHeight() - inset * 2 - 1);
                    g2.dispose();
                }
            };
            cb.setPreferredSize(new Dimension(32, 32));
            cb.setOpaque(false); cb.setContentAreaFilled(false);
            cb.setBorderPainted(false); cb.setFocusPainted(false);
            cb.setRolloverEnabled(true);
            cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cb.setToolTipText(lcName);
            if (i == 0) cb.setSelected(true);
            colorGroup.add(cb);
            cb.addActionListener(e -> {
                selectedLightColor = lc;
            });
            colorGrid.add(cb, "w 32!, h 32!");
        }
        card.add(colorGrid);

        // ── Intensity + Angle sliders ──
        card.add(new JSeparator(), "growx, gaptop 2, gapbottom 2");
        card.add(sliderRow("Brightness", intensitySlider, "%"), "growx");
        card.add(sliderHint("Lower for ambient glow, higher for strong lighting."), "growx, gapbottom 4");
        card.add(sliderRow("Beam Direction", directionSlider, "\u00B0"), "growx");
        card.add(sliderHint("0 to 360 degrees rotates the direction of the light."), "growx");

        // ── Buttons row ──
        JPanel btnRow = new JPanel(new MigLayout("insets 0", "[grow]"));
        btnRow.setOpaque(false);

        JButton addBtn = new JButton("Add Light");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBackground(GREEN); addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false); addBtn.setBorderPainted(false); addBtn.setOpaque(true);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setToolTipText("Add the selected light to your room as a 3D object");
        addBtn.addActionListener(e -> {
            if (selectedLightType == null) {
                JOptionPane.showMessageDialog(this, "Please select a light type first.",
                        "Select Light", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            addLightToScene = true;
            confirmed = true;
            intensity  = intensitySlider.getValue() / 100.0;
            direction  = directionSlider.getValue();
            dispose();
        });
        btnRow.add(addBtn, "growx, h 36!");
        card.add(btnRow, "growx, gaptop 12");

        return card;
    }

    private void showFavourites() {
        gridContainer.removeAll();
        JPanel grid = new JPanel(new MigLayout("wrap 3, gapy 8, gapx 8, insets 0",
                "[grow, fill][grow, fill][grow, fill]"));
        grid.setOpaque(false);
        for (int idx : FAVOURITES) {
            grid.add(createLightButton(idx), "h 80!, grow");
        }
        gridContainer.add(grid, "grow");
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private void showCategories() {
        gridContainer.removeAll();
        JPanel catPanel = new JPanel(new MigLayout("wrap 1, insets 0, gapy 8", "[grow, fill]"));
        catPanel.setOpaque(false);
        for (int ci = 0; ci < CATEGORY_NAMES.length; ci++) {
            JLabel catLabel = new JLabel(CATEGORY_NAMES[ci]);
            catLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            catLabel.setForeground(new Color(100, 100, 100));
            catPanel.add(catLabel);
            JPanel grid = new JPanel(new MigLayout("wrap 3, gapy 8, gapx 8, insets 0",
                    "[grow, fill][grow, fill][grow, fill]"));
            grid.setOpaque(false);
            for (int idx : CATEGORY_ITEMS[ci]) {
                grid.add(createLightButton(idx), "h 80!, grow");
            }
            catPanel.add(grid, "grow");
        }
        gridContainer.add(catPanel, "grow");
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private void updateTabStyles() {
        favBtn.setForeground(showingFavourites ? Color.WHITE : Color.BLACK);
        catBtn.setForeground(!showingFavourites ? Color.WHITE : Color.BLACK);
        favBtn.repaint();
        catBtn.repaint();
    }

    private JToggleButton createLightButton(int itemIndex) {
        String name = (String) LIGHT_ITEMS[itemIndex][0];
        Furniture.Type type = (Furniture.Type) LIGHT_ITEMS[itemIndex][1];
        JToggleButton btn = new JToggleButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isSelected() ? new Color(41, 180, 76, 30) : new Color(248, 248, 248);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(isSelected() ? GREEN : CARD_BORDER);
                g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                drawLightIcon(g2, itemIndex, getWidth(), (int)(getHeight() * 0.68));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                g2.setColor(new Color(60, 60, 60));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(name, (getWidth() - fm.stringWidth(name)) / 2, getHeight()-4);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(0, 80));
        btn.setOpaque(false); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lightSelGroup.add(btn);
        btn.addActionListener(e -> selectedLightType = type);
        return btn;
    }

    private void drawLightIcon(Graphics2D g2, int itemIndex, int pw, int ih) {
        int cx = pw / 2, cy = ih / 2;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color bulb = new Color(255, 230, 100);
        Color body = new Color(150, 120, 80);
        switch (itemIndex) {
            case 0 -> { // Floor Lamp
                g2.setColor(body);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(cx, ih - 6, cx, cy);
                g2.fillOval(cx - 16, cy - 14, 32, 16);
                g2.setColor(bulb);
                g2.fillOval(cx - 5, cy - 8, 10, 10);
            }
            case 1 -> { // Ceiling
                g2.setColor(body);
                g2.fillRoundRect(cx - 16, 4, 32, 12, 6, 6);
                g2.setColor(bulb);
                for (int i = -2; i <= 2; i++) {
                    g2.fillOval(cx + i * 10 - 4, 18, 8, 8);
                }
            }
            case 2 -> { // Spot
                g2.setColor(new Color(180, 180, 180));
                int[] xp = {cx - 10, cx + 10, cx + 6, cx - 6};
                int[] yp = {cy - 14, cy - 14, cy + 4, cy + 4};
                g2.fillPolygon(xp, yp, 4);
                g2.setColor(bulb);
                g2.fillOval(cx - 5, cy - 2, 10, 10);
            }
            case 3 -> { // Table Lamp
                g2.setColor(new Color(200, 130, 80));
                int[] xs = {cx - 14, cx + 14, cx + 8, cx - 8};
                int[] ys = {cy - 14, cy - 14, cy + 2, cy + 2};
                g2.fillPolygon(xs, ys, 4);
                g2.setColor(body);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(cx, cy + 2, cx, cy + 14);
                g2.fillOval(cx - 8, cy + 14, 16, 6);
                g2.setColor(bulb);
                g2.fillOval(cx - 4, cy - 4, 8, 8);
            }
        }
    }

    // ── Utilities ──

    private JButton makeCloseButton() {
        JButton close = new JButton() {
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
        close.setRolloverEnabled(true);
        close.setPreferredSize(new Dimension(28, 28));
        close.setFocusPainted(false); close.setBorderPainted(false);
        close.setContentAreaFilled(false); close.setOpaque(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());
        return close;
    }

    private JPanel sliderRow(String labelText, JSlider slider, String unit) {
        JPanel row = new JPanel(new MigLayout("insets 0", "[grow, fill][]"));
        row.setOpaque(false);
        slider.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        JLabel val = new JLabel(slider.getValue() + unit);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        val.setPreferredSize(new Dimension(38, 16));
        slider.addChangeListener(e -> val.setText(slider.getValue() + unit));
        JPanel col = new JPanel(new MigLayout("wrap 1, insets 0, gapy 2", "[grow, fill]"));
        col.setOpaque(false);
        col.add(lbl); col.add(slider);
        row.add(col, "grow");
        row.add(val, "al right");
        return row;
    }

    private JLabel sliderHint(String text) {
        JLabel hint = new JLabel(text);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hint.setForeground(new Color(120, 120, 120));
        return hint;
    }

    private void configureSliders() {
        intensitySlider.setPaintTicks(true);
        intensitySlider.setMajorTickSpacing(25);
        intensitySlider.setMinorTickSpacing(5);
        intensitySlider.setToolTipText("Brightness percentage");

        directionSlider.setPaintTicks(true);
        directionSlider.setMajorTickSpacing(90);
        directionSlider.setMinorTickSpacing(15);
        directionSlider.setToolTipText("Direction in degrees");
    }

    private JToggleButton tabButton(String text, boolean active) {
        JToggleButton btn = new JToggleButton(text) {
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
        btn.setSelected(active);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        btn.setForeground(active ? Color.WHITE : Color.BLACK);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        return btn;
    }

    // ── Public API ──

    public boolean isConfirmed()        { return confirmed; }
    public double getIntensity()        { return intensity; }
    public double getDirection()        { return direction; }
    public boolean isAddLightToScene()  { return addLightToScene; }
    public Furniture.Type getSelectedLightType() { return selectedLightType; }
    public Color getSelectedLightColor() { return selectedLightColor; }
}

