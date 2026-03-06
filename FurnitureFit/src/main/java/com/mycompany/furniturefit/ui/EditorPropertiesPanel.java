package com.mycompany.furniturefit.ui;

import com.mycompany.furniturefit.graphics.Canvas2DPanel;
import com.mycompany.furniturefit.graphics.Canvas3DPanel;
import com.mycompany.furniturefit.graphics.OpenGLCanvas3D;
import com.mycompany.furniturefit.model.Furniture;
import com.mycompany.furniturefit.model.Room;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Extracted floating properties panel for the design editor.
 * Displays furniture/room properties: size, brightness, color picker.
 */
public class EditorPropertiesPanel extends JPanel {

    private static final Color GREEN = new Color(45, 136, 45);
    private static final Color TOOLBAR_FG = new Color(50, 50, 50);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER = new Color(210, 210, 210);

    private final Canvas2DPanel canvas2DPanel;
    private final Canvas3DPanel canvas3DPanel;
    private final OpenGLCanvas3D openGLCanvas3D;
    private Runnable onPushUndo;
    private Room selectedRoom;

    // â”€â”€ UI Components â”€â”€
    private JPanel propContentPanel;
    private JLabel propTitleLabel;
    private JSpinner propWidthSpinner, propDepthSpinner, propHeightSpinner;
    private JSlider propBrightnessSlider;
    private JLabel propBrightnessLabel;
    private JToggleButton propLightOnBtn;
    private JPanel propLightOnPanel;
    private boolean propUpdating = false;

    public EditorPropertiesPanel(Canvas2DPanel canvas2DPanel, Canvas3DPanel canvas3DPanel, 
                                  OpenGLCanvas3D openGLCanvas3D, Runnable onPushUndo) {
        this.canvas2DPanel = canvas2DPanel;
        this.canvas3DPanel = canvas3DPanel;
        this.openGLCanvas3D = openGLCanvas3D;
        this.onPushUndo = onPushUndo;
        
        setLayout(new BorderLayout());
        setOpaque(false);
        setVisible(false);
        
        add(createFloatingProperties(), BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return this;
    }

    private JPanel createFloatingProperties() {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 14", "[grow, fill]")) {
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
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(280, 600)); 

        // Header
        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]"));
        header.setOpaque(false);
        propTitleLabel = new JLabel("Properties");
        propTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        propTitleLabel.setForeground(TOOLBAR_FG);
        header.add(propTitleLabel);
        
        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setForeground(new Color(100, 100, 100));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> setVisible(false));
        header.add(closeBtn);
        card.add(header);

        // Content panel
        propContentPanel = new JPanel(new MigLayout("wrap 2, insets 0, gapy 4, gapx 6", "[left, 70!][left]"));
        propContentPanel.setOpaque(false);
        card.add(propContentPanel);

        // â”€â”€ Size controls section â”€â”€
        card.add(new JSeparator(), "growx, gaptop 6, gapbottom 4");
        JLabel sizeLabel = new JLabel("Dimensions (cm)");
        sizeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sizeLabel.setForeground(TOOLBAR_FG);
        card.add(sizeLabel);

        JPanel sizePanel = createSizePanel();
        card.add(sizePanel, "alignx left");

        // â”€â”€ Brightness slider â”€â”€
        card.add(new JSeparator(), "growx, gaptop 6, gapbottom 4");
        card.add(createBrightnessRow());

        // â”€â”€ Light On/Off toggle â”€â”€
        propLightOnPanel = createLightOnPanel();
        card.add(new JSeparator(), "growx, gaptop 6, gapbottom 4");
        card.add(propLightOnPanel);

        // â”€â”€ Color picker â”€â”€
        card.add(new JSeparator(), "growx, gaptop 8, gapbottom 4");
        JLabel pickLabel = new JLabel("Main Colors");
        pickLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pickLabel.setForeground(TOOLBAR_FG);
        card.add(pickLabel);

        card.add(createColorGrid());

        return card;
    }

    private JPanel createSizePanel() {
        JPanel sizePanel = new JPanel(new MigLayout("wrap 3, insets 0, gapy 6, gapx 6", "[left, 80!][110!][30!]"));
        sizePanel.setOpaque(false);

        propWidthSpinner = createPropSpinner(10, 1000, 1);
        propDepthSpinner = createPropSpinner(10, 1000, 1);
        propHeightSpinner = createPropSpinner(10, 500, 1);

        sizePanel.add(createSmallLabel("Width"), "alignx left");
        sizePanel.add(propWidthSpinner, "w 110!");
        sizePanel.add(createSmallLabel("cm"), "alignx left");
        sizePanel.add(createSmallLabel("Depth"), "alignx left");
        sizePanel.add(propDepthSpinner, "w 110!");
        sizePanel.add(createSmallLabel("cm"), "alignx left");
        sizePanel.add(createSmallLabel("Height"), "alignx left");
        sizePanel.add(propHeightSpinner, "w 110!");
        sizePanel.add(createSmallLabel("cm"), "alignx left");

        return sizePanel;
    }

    private JPanel createBrightnessRow() {
        JPanel brightnessRow = new JPanel(new MigLayout("insets 0", "[grow][]"));
        brightnessRow.setOpaque(false);
        
        JLabel brightnessTitle = new JLabel("Brightness");
        brightnessTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        brightnessTitle.setForeground(TOOLBAR_FG);
        propBrightnessLabel = new JLabel("0");
        propBrightnessLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        propBrightnessLabel.setForeground(new Color(120, 120, 120));
        brightnessRow.add(brightnessTitle);
        brightnessRow.add(propBrightnessLabel, "al right");

        propBrightnessSlider = new JSlider(-100, 100, 0);
        propBrightnessSlider.setOpaque(false);
        propBrightnessSlider.setMajorTickSpacing(50);
        propBrightnessSlider.setPaintTicks(true);
        propBrightnessSlider.addChangeListener(e -> {
            if (!propUpdating) {
                Furniture sel = canvas2DPanel.getSelectedFurniture();
                if (sel != null) {
                    if (!propBrightnessSlider.getValueIsAdjusting()) {
                        if (onPushUndo != null) onPushUndo.run();
                    }
                    sel.setBrightness(propBrightnessSlider.getValue() / 100.0);
                    int v = propBrightnessSlider.getValue();
                    propBrightnessLabel.setText(v > 0 ? "+" + v : String.valueOf(v));
                    canvas2DPanel.repaint();
                    canvas3DPanel.repaint();
                    openGLCanvas3D.repaint();
                }
            }
        });
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(brightnessRow, BorderLayout.NORTH);
        wrapper.add(propBrightnessSlider, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createLightOnPanel() {
        JPanel propLightOnPanel = new JPanel(new MigLayout("insets 0", "[grow][]"));
        propLightOnPanel.setOpaque(false);
        propLightOnPanel.setVisible(false);
        
        JLabel lightLabel = new JLabel("Lamp Light");
        lightLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lightLabel.setForeground(TOOLBAR_FG);
        propLightOnPanel.add(lightLabel);

        propLightOnBtn = new JToggleButton("OFF") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean on = isSelected();
                g2.setColor(on ? new Color(255, 200, 50) : new Color(200, 200, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                int knobD = getHeight() - 4;
                int knobX = on ? getWidth() - knobD - 2 : 2;
                g2.setColor(Color.WHITE);
                g2.fillOval(knobX, 2, knobD, knobD);
                g2.setColor(on ? new Color(80, 60, 0) : new Color(100, 100, 100));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                String txt = on ? "ON" : "OFF";
                int tx = on ? 6 : getWidth() - fm.stringWidth(txt) - 6;
                g2.drawString(txt, tx, (getHeight() + fm.getAscent()) / 2 - 2);
                g2.dispose();
            }
        };
        propLightOnBtn.setPreferredSize(new Dimension(52, 24));
        propLightOnBtn.setFocusPainted(false);
        propLightOnBtn.setBorderPainted(false);
        propLightOnBtn.setContentAreaFilled(false);
        propLightOnBtn.setOpaque(false);
        propLightOnBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        propLightOnBtn.addActionListener(e -> {
            if (!propUpdating) {
                Furniture sel = canvas2DPanel.getSelectedFurniture();
                if (sel != null && sel.getType().isLightFixture()) {
                    if (!propLightOnBtn.isSelected()) {
                        sel.setBrightness(0);
                    }
                    if (onPushUndo != null) onPushUndo.run();
                    sel.setLightOn(propLightOnBtn.isSelected());
                    canvas2DPanel.repaint();
                    canvas3DPanel.repaint();
                    openGLCanvas3D.repaint();
                }
            }
        });
        propLightOnPanel.add(propLightOnBtn, "al right");
        
        return propLightOnPanel;
    }

    private JLabel createSmallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(100, 100, 100));
        return l;
    }

    private JSpinner createPropSpinner(int minCm, int maxCm, int stepCm) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(minCm, minCm, maxCm, stepCm));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        spinner.addChangeListener(e -> {
            if (!propUpdating) {
                double widthM = ((int) propWidthSpinner.getValue()) / 100.0;
                double depthM = ((int) propDepthSpinner.getValue()) / 100.0;
                double heightM = ((int) propHeightSpinner.getValue()) / 100.0;
                Furniture sel = canvas2DPanel.getSelectedFurniture();
                if (sel != null) {
                    if (onPushUndo != null) onPushUndo.run();
                    sel.setWidth(widthM);
                    sel.setDepth(depthM);
                    sel.setHeight(heightM);
                    canvas2DPanel.repaint();
                    canvas3DPanel.repaint();
                    openGLCanvas3D.repaint();
                } else if (selectedRoom != null) {
                    if (onPushUndo != null) onPushUndo.run();
                    selectedRoom.setWidth(widthM);
                    selectedRoom.setDepth(depthM);
                    selectedRoom.setHeight(heightM);
                    canvas2DPanel.repaint();
                    canvas3DPanel.repaint();
                    openGLCanvas3D.repaint();
                }
            }
        });
        return spinner;
    }

    private JPanel createColorGrid() {
        Color[] palette = {
            new Color(245, 245, 240), // Ivory
            new Color(226, 219, 206), // Linen
            new Color(196, 181, 158), // Taupe
            new Color(160, 144, 122), // Stone
            new Color(96, 86, 74),    // Charcoal Brown
            new Color(201, 155, 103), // Oak
            new Color(154, 108, 67),  // Walnut
            new Color(112, 77, 48),   // Teak
            new Color(84, 57, 38),    // Espresso
            new Color(32, 32, 32),    // Black
            new Color(116, 129, 140), // Cool Gray
            new Color(72, 104, 141),  // Denim Blue
            new Color(76, 118, 88),   // Olive Green
            new Color(136, 93, 58),   // Clay
            new Color(176, 64, 47)    // Terracotta
        };

        JPanel grid = new JPanel(new MigLayout("wrap 5, insets 6, gap 6", "[grow,fill][grow,fill][grow,fill][grow,fill][grow,fill]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 248, 248));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        grid.setOpaque(false);

        for (Color c : palette) {
            JButton cb = createColorButton(c);
            grid.add(cb, "w 32!, h 32!, alignx center");
        }

        // Custom color button
        JButton customBtn = createCustomColorButton();
        grid.add(customBtn, "span 5, growx, gaptop 6, h 30!");

        return grid;
    }

    private JButton createColorButton(Color c) {
        JButton cb = new JButton() {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                int inset = hovered ? 0 : 2;
                if (hovered) {
                    g2.setColor(new Color(0, 0, 0, 40));
                    g2.fillRoundRect(x + 1, y + 1, size, size, 8, 8);
                }
                g2.setColor(c);
                g2.fillRoundRect(x + inset, y + inset, size - 2 * inset, size - 2 * inset, 8, 8);
                g2.setColor(hovered ? GREEN : new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(hovered ? 2f : 1f));
                g2.drawRoundRect(x + inset, y + inset, size - 2 * inset - 1, size - 2 * inset - 1, 8, 8);
                g2.dispose();
            }
        };
        cb.setPreferredSize(new Dimension(32, 32));
        cb.setFocusPainted(false);
        cb.setBorderPainted(false);
        cb.setContentAreaFilled(false);
        cb.setOpaque(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.setToolTipText(String.format("RGB(%d, %d, %d)", c.getRed(), c.getGreen(), c.getBlue()));
        cb.addActionListener(e -> applyColorToSelection(c));
        return cb;
    }

    private JButton createCustomColorButton() {
        JButton customBtn = new JButton("Custom Color...") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hov = getModel().isRollover();
                g2.setColor(hov ? GREEN.darker() : GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                String t = "Custom Color...";
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2, (getHeight() + fm.getAscent()) / 2 - 2);
                g2.dispose();
            }
        };
        customBtn.setPreferredSize(new Dimension(0, 26));
        customBtn.setOpaque(false);
        customBtn.setContentAreaFilled(false);
        customBtn.setBorderPainted(false);
        customBtn.setFocusPainted(false);
        customBtn.setRolloverEnabled(true);
        customBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        customBtn.addActionListener(e -> {
            Furniture sel = canvas2DPanel.getSelectedFurniture();
            Color currentCol = Color.WHITE;
            if (sel != null) {
                currentCol = sel.getColor();
            } else if (selectedRoom != null) {
                currentCol = selectedRoom.getFloorColor();
            }
            Color picked = JColorChooser.showDialog(this, "Choose Color", currentCol);
            if (picked != null) applyColorToSelection(picked);
        });
        return customBtn;
    }

    public void applyColorToSelection(Color c) {
        Furniture sel = canvas2DPanel.getSelectedFurniture();
        if (sel != null) {
            if (onPushUndo != null) onPushUndo.run();
            sel.setColor(c);
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
            return;
        }

        if (selectedRoom != null) {
            if (onPushUndo != null) onPushUndo.run();
            selectedRoom.setFloorColor(c);
            selectedRoom.setWallColor(lightenColor(c, 0.32));
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
        }
    }

    public void showPropertiesForFurniture(Furniture f) {
        selectedRoom = null;
        propContentPanel.removeAll();
        if (f == null) {
            setVisible(false);
            return;
        }
        propTitleLabel.setText("Properties");
        long price = getPrice(f.getType());
        addPropRow("Price :", "Rs. " + String.format("%,d", price));
        addPropRow("Name :", f.getName());
        addPropRow("Height :", String.format("%.0f cm", f.getHeight() * 100));
        addPropRow("Width :", String.format("%.0f cm", f.getWidth() * 100));

        // Update resize spinners
        propUpdating = true;
        propWidthSpinner.setValue((int) Math.round(f.getWidth() * 100.0));
        propDepthSpinner.setValue((int) Math.round(f.getDepth() * 100.0));
        propHeightSpinner.setValue((int) Math.round(f.getHeight() * 100.0));
        int bVal = (int) (f.getBrightness() * 100);
        propBrightnessSlider.setValue(bVal);
        propBrightnessLabel.setText(bVal > 0 ? "+" + bVal : String.valueOf(bVal));
        propBrightnessSlider.setEnabled(true);

        // Show/hide lamp light toggle
        boolean isLamp = f.getType().isLightFixture();
        propLightOnPanel.setVisible(isLamp);
        if (isLamp) {
            propLightOnBtn.setSelected(f.isLightOn());
        }

        propUpdating = false;

        setVisible(true);
        revalidate();
        repaint();
    }

    public void showPropertiesForRoom(Room room) {
        if (room == null) return;
        selectedRoom = room;
        propContentPanel.removeAll();
        propTitleLabel.setText("Properties");
        addPropRow("Name :", room.getShape().getDisplayName() + " room");
        addPropRow("Height :", String.format("%.0f cm", room.getHeight() * 100));
        addPropRow("Width :", String.format("%.0f cm", room.getWidth() * 100));

        propUpdating = true;
        propWidthSpinner.setValue((int) Math.round(room.getWidth() * 100.0));
        propDepthSpinner.setValue((int) Math.round(room.getDepth() * 100.0));
        propHeightSpinner.setValue((int) Math.round(room.getHeight() * 100.0));
        propBrightnessSlider.setEnabled(false);
        propBrightnessLabel.setText("N/A");
        propLightOnPanel.setVisible(false);
        propUpdating = false;

        setVisible(true);
        revalidate();
        repaint();
    }

    public void hideProperties() {
        setVisible(false);
    }

    private void addPropRow(String label, String value) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(80, 80, 80));
        propContentPanel.add(lbl, "alignx left");
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        val.setForeground(TOOLBAR_FG);
        propContentPanel.add(val, "alignx left");
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    private long getPrice(Furniture.Type type) {
        return switch (type) {
            case CHAIR -> 15000;
            case DINING_TABLE -> 45000;
            case SIDE_TABLE -> 10000;
            case SOFA -> 110000;
            case SHELF -> 25000;
            case COFFEE_TABLE -> 20000;
            case BED -> 120000;
            case WARDROBE -> 85000;
            case DESK -> 35000;
            case LAMP -> 8000;
            case PENDANT_LIGHT -> 0;
            case FLOOR_LAMP_LIGHT -> 0;
            case CEILING_LIGHT -> 0;
            case WALL_LIGHT -> 0;
            case SPOTLIGHT -> 0;
            case TABLE_LAMP_LIGHT -> 0;
        };
    }

    private Color lightenColor(Color c, double amount) {
        int r = c.getRed() + (int) ((255 - c.getRed()) * amount);
        int g = c.getGreen() + (int) ((255 - c.getGreen()) * amount);
        int b = c.getBlue() + (int) ((255 - c.getBlue()) * amount);
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }
}

