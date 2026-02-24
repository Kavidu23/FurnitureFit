package com.mycompany.furnituredesignapp.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Lights picker and shade configurator shown as a floating panel
 * matching the "Lights" side panel in the screenshots:
 *   – "Lights" title + X close
 *   – Favourites / Categories tabs
 *   – 2-column grid of light-fixture thumbnails
 *   – Compact intensity / direction sliders below
 */
public class LightingDialog extends JDialog {

    private static final Color GREEN       = new Color(56, 124, 43);
    private static final Color CARD_BG     = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(210, 210, 210);

    private final JSlider intensitySlider;
    private final JSlider directionSlider;
    private boolean confirmed = false;
    private boolean applyToAll = false;
    private double intensity = 0.3;
    private double direction = 45;

    private enum LightType {
        PENDANT("Pendant"), FLOOR_LAMP("Floor Lamp"), CEILING("Ceiling"),
        WALL_LIGHT("Wall Light"), SPOT("Spotlight"), TABLE_LAMP("Table Lamp");
        final String displayName;
        LightType(String n) { displayName = n; }
    }

    public LightingDialog(Frame owner) {
        super(owner, true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(310, 520);
        if (owner != null) {
            Point loc = owner.getLocationOnScreen();
            setLocation(loc.x + 14, loc.y + (owner.getHeight() - 520) / 2);
        }

        intensitySlider = new JSlider(0, 100, 30);
        directionSlider = new JSlider(0, 360, 45);

        setContentPane(buildPanel());
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
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
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth()-1, getHeight()-1, 18, 18));
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]"));
        header.setOpaque(false);
        JLabel title = new JLabel("Lights");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(40, 40, 40));
        header.add(title);
        JButton close = new JButton("\u2715");
        close.setFont(new Font("Segoe UI", Font.BOLD, 13));
        close.setForeground(new Color(100, 100, 100));
        close.setFocusPainted(false); close.setBorderPainted(false); close.setContentAreaFilled(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());
        header.add(close);
        card.add(header, "growx");

        // ── Tabs ─────────────────────────────────────────────────────────────
        JPanel tabs = new JPanel(new MigLayout("insets 0, gap 6", "[][]"));
        tabs.setOpaque(false);
        JToggleButton favBtn = tabButton("Favourites", true);
        JToggleButton catBtn = tabButton("Categories", false);
        ButtonGroup tg = new ButtonGroup();
        tg.add(favBtn); tg.add(catBtn);
        tabs.add(favBtn, "h 28!"); tabs.add(catBtn, "h 28!");
        card.add(tabs);

        // ── Light fixture thumbnails grid ─────────────────────────────────────
        JPanel grid = new JPanel(new MigLayout("wrap 2, gapy 8, gapx 8, insets 0",
                "[grow, fill][grow, fill]"));
        grid.setOpaque(false);
        ButtonGroup selGroup = new ButtonGroup();
        for (LightType lt : LightType.values()) {
            JToggleButton btn = createLightButton(lt);
            selGroup.add(btn);
            grid.add(btn, "h 80!, grow");
        }
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null); scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, "grow, push, h 230!");

        // ── Intensity slider ──────────────────────────────────────────────────
        card.add(sliderRow("Shade Intensity", intensitySlider, "%"), "growx");

        // ── Direction slider ──────────────────────────────────────────────────
        card.add(sliderRow("Light Angle", directionSlider, "°"), "growx");

        // ── Scope radio + Apply ───────────────────────────────────────────────
        JPanel scope = new JPanel(new MigLayout("insets 0, gap 6", "[][]"));
        scope.setOpaque(false);
        JRadioButton selOnly = new JRadioButton("Selected only", true);
        JRadioButton allFurn = new JRadioButton("All furniture");
        selOnly.setOpaque(false); allFurn.setOpaque(false);
        selOnly.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        allFurn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ButtonGroup sg = new ButtonGroup(); sg.add(selOnly); sg.add(allFurn);
        scope.add(selOnly); scope.add(allFurn);
        card.add(scope, "growx");

        JButton applyBtn = new JButton("Apply");
        applyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        applyBtn.setBackground(GREEN); applyBtn.setForeground(Color.WHITE);
        applyBtn.setFocusPainted(false); applyBtn.setBorderPainted(false);
        applyBtn.setOpaque(true);
        applyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyBtn.addActionListener(e -> {
            confirmed = true;
            intensity  = intensitySlider.getValue() / 100.0;
            direction  = directionSlider.getValue();
            applyToAll = allFurn.isSelected();
            dispose();
        });
        card.add(applyBtn, "growx, h 34!");

        return card;
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

    private JToggleButton tabButton(String text, boolean active) {
        JToggleButton btn = new JToggleButton(text);
        btn.setSelected(active);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        btn.setBackground(active ? GREEN : new Color(230, 230, 230));
        btn.setForeground(active ? Color.WHITE : new Color(80, 80, 80));
        btn.setContentAreaFilled(true); btn.setOpaque(true);
        btn.addActionListener(e -> {
            btn.setBackground(btn.isSelected() ? GREEN : new Color(230, 230, 230));
            btn.setForeground(btn.isSelected() ? Color.WHITE : new Color(80, 80, 80));
        });
        return btn;
    }

    private JToggleButton createLightButton(LightType lt) {
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
                drawLightIcon(g2, lt, getWidth(), (int)(getHeight() * 0.72));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(60, 60, 60));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lt.displayName, (getWidth() - fm.stringWidth(lt.displayName)) / 2, getHeight()-5);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(0, 80));
        btn.setOpaque(false); btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void drawLightIcon(Graphics2D g2, LightType lt, int pw, int ih) {
        int cx = pw / 2, cy = ih / 2;
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color bulb = new Color(255, 230, 100);
        Color body = new Color(150, 120, 80);
        switch (lt) {
            case PENDANT -> {
                g2.setColor(new Color(180, 160, 120));
                g2.drawLine(cx, 4, cx, cy - 10);
                g2.setColor(body);
                g2.fillOval(cx - 12, cy - 12, 24, 20);
                g2.setColor(bulb);
                g2.fillOval(cx - 7, cy - 5, 14, 14);
            }
            case FLOOR_LAMP -> {
                g2.setColor(body);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(cx, ih - 6, cx, cy);
                g2.fillOval(cx - 16, cy - 14, 32, 16);
                g2.setColor(bulb);
                g2.fillOval(cx - 5, cy - 8, 10, 10);
            }
            case CEILING -> {
                g2.setColor(body);
                g2.fillRoundRect(cx - 16, 4, 32, 12, 6, 6);
                g2.setColor(bulb);
                for (int i = -2; i <= 2; i++) {
                    g2.fillOval(cx + i * 10 - 4, 18, 8, 8);
                }
            }
            case WALL_LIGHT -> {
                g2.setColor(body);
                g2.fillRect(cx - 4, cy - 14, 8, 20);
                g2.fillOval(cx - 12, cy + 2, 24, 14);
                g2.setColor(bulb);
                g2.fillOval(cx - 6, cy + 6, 12, 12);
            }
            case SPOT -> {
                g2.setColor(new Color(180, 180, 180));
                int[] xp = {cx - 10, cx + 10, cx + 6, cx - 6};
                int[] yp = {cy - 14, cy - 14, cy + 4, cy + 4};
                g2.fillPolygon(xp, yp, 4);
                g2.setColor(bulb);
                g2.fillOval(cx - 5, cy - 2, 10, 10);
            }
            case TABLE_LAMP -> {
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

    public boolean isConfirmed() { return confirmed; }
    public double getIntensity() { return intensity; }
    public double getDirection() { return direction; }
    public boolean isApplyToAll() { return applyToAll; }
}
