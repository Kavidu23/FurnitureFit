package com.mycompany.furniturefit.graphics;

import com.mycompany.furniturefit.model.Design;
import com.mycompany.furniturefit.model.Furniture;
import com.mycompany.furniturefit.model.Room;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * 2D top-down view canvas for furniture design.
 * Renders the room floor plan with furniture items that can be selected, moved, and manipulated.
 */
public class Canvas2DPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

    private Design design;
    private Furniture selectedFurniture;
    private final Image rotateHandleImage;
    private boolean roomSelected = false;
    private boolean roomVisible = true;
    private double zoom = 1.0;
    private double panX = 0, panY = 0;
    private double pixelsPerMeter = 80.0; // Scale: 80 pixels = 1 meter

    // Drag state
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;
    private boolean panning = false;
    private int panStartX, panStartY;

    // Callbacks
    private Runnable onSelectionChanged;
    private Runnable onDesignModified;
    private Runnable onBeforeModified;  // fired before any destructive change (for undo)
    private Runnable onRoomSelected;

    public Canvas2DPanel() {
        setBackground(Color.WHITE);   // white canvas for 2D design view
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setFocusable(true);
        rotateHandleImage = loadRotateHandleImage();

        // Keyboard shortcuts
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (selectedFurniture != null) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DELETE -> {
                            if (design != null) {
                                notifyBeforeModified();
                                design.removeFurniture(selectedFurniture);
                                selectedFurniture = null;
                                notifySelectionChanged();
                                notifyDesignModified();
                                repaint();
                            }
                        }
                        case KeyEvent.VK_R -> {
                            notifyBeforeModified();
                            selectedFurniture.setRotation(selectedFurniture.getRotation() + 15);
                            notifyDesignModified();
                            repaint();
                        }
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_DELETE && selectedFurniture == null && roomSelected && roomVisible) {
                    notifyBeforeModified();
                    if (design != null && design.getFurnitureList() != null) {
                        design.getFurnitureList().clear(); // delete all furniture/lights with the room
                    }
                    roomVisible = false;
                    roomSelected = false;
                    notifySelectionChanged();
                    notifyDesignModified();
                    repaint();
                }
                if (e.getKeyCode() == KeyEvent.VK_0 && e.isControlDown()) {
                    zoom = 1.0;
                    panX = 0;
                    panY = 0;
                    repaint();
                }
            }
        });
    }

    public void setDesign(Design design) {
        this.design = design;
        this.selectedFurniture = null;
        this.roomSelected = false;
        this.zoom = 1.0;
        this.panX = 0;
        this.panY = 0;
        repaint();
    }

    public Design getDesign() {
        return design;
    }

    public void setSelectedFurniture(Furniture furniture) {
        this.selectedFurniture = furniture;
        repaint();
    }

    public Furniture getSelectedFurniture() {
        return selectedFurniture;
    }

    public void setRoomVisible(boolean roomVisible) {
        this.roomVisible = roomVisible;
        if (!roomVisible) {
            roomSelected = false;
        }
        repaint();
    }

    public boolean isRoomVisible() {
        return roomVisible;
    }

    public void setOnSelectionChanged(Runnable callback) {
        this.onSelectionChanged = callback;
    }

    public void setOnDesignModified(Runnable callback) {
        this.onDesignModified = callback;
    }

    public void setOnBeforeModified(Runnable callback) {
        this.onBeforeModified = callback;
    }

    public void setOnRoomSelected(Runnable callback) {
        this.onRoomSelected = callback;
    }

    public boolean isRoomSelected() { return roomSelected && selectedFurniture == null; }

    private void notifySelectionChanged() {
        if (onSelectionChanged != null) onSelectionChanged.run();
    }

    private void notifyRoomSelected() {
        if (onRoomSelected != null) onRoomSelected.run();
    }

    private void notifyDesignModified() {
        if (onDesignModified != null) onDesignModified.run();
    }

    private void notifyBeforeModified() {
        if (onBeforeModified != null) onBeforeModified.run();
    }

    /**
     * Convert screen coordinates to room coordinates.
     */
    private double[] screenToRoom(int sx, int sy) {
        double cx = getWidth() / 2.0 + panX;
        double cy = getHeight() / 2.0 + panY;
        double scale = pixelsPerMeter * zoom;

        double roomX = (sx - cx) / scale;
        double roomY = (sy - cy) / scale;
        return new double[]{roomX, roomY};
    }

    /**
     * Convert room coordinates to screen coordinates.
     */
    private int[] roomToScreen(double rx, double ry) {
        double cx = getWidth() / 2.0 + panX;
        double cy = getHeight() / 2.0 + panY;
        double scale = pixelsPerMeter * zoom;

        int sx = (int) (cx + rx * scale);
        int sy = (int) (cy + ry * scale);
        return new int[]{sx, sy};
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (design == null) {
            setBackground(new Color(75, 75, 75));
            super.paintComponent(g);
            drawPlaceholder(g);
            return;
        }
        setBackground(Color.WHITE);
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        double cx = getWidth() / 2.0 + panX;
        double cy = getHeight() / 2.0 + panY;
        double scale = pixelsPerMeter * zoom;

        // Transform to center room
        g2d.translate(cx, cy);
        g2d.scale(scale, scale);

        // Draw grid
        drawGrid(g2d, scale);

        // Draw room (with green border if room selected)
        if (roomVisible && design.getRoom() != null) {
            drawRoom(g2d);
        }

        // Draw furniture items
        drawFurniture(g2d);

        // Draw distance measurements for selected item
        if (selectedFurniture != null && design.getRoom() != null) {
            drawDistanceMeasurements(g2d, selectedFurniture);
        }

        g2d.dispose();

        // Draw HUD overlay (zoom level, room info)
        drawHUD(g);

        // Draw screen-space overlays (rotation handle)
        Graphics2D gScreen = (Graphics2D) g.create();
        gScreen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawScreenOverlays(gScreen);
        gScreen.dispose();
    }

    private void drawPlaceholder(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(180, 180, 180));
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        String msg = "Create a new design or open an existing one to start";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
    }

    private void drawGrid(Graphics2D g2d, double scale) {
        // Fine line graph-paper grid on white background
        double gridStep = 0.5; // 0.5-metre spacing
        double gridExtent = 25.0;
        g2d.setColor(new Color(200, 200, 200, 180));
        float lineW = (float)(0.7 / scale);
        g2d.setStroke(new BasicStroke(lineW));
        for (double gx = -gridExtent; gx <= gridExtent; gx += gridStep) {
            g2d.draw(new java.awt.geom.Line2D.Double(gx, -gridExtent, gx, gridExtent));
        }
        for (double gy = -gridExtent; gy <= gridExtent; gy += gridStep) {
            g2d.draw(new java.awt.geom.Line2D.Double(-gridExtent, gy, gridExtent, gy));
        }
    }

    private void drawRoom(Graphics2D g2d) {
        Room room = design.getRoom();
        double w = room.getWidth();
        double d = room.getDepth();

        // ─ Wall border (dark slate, thick) ─
        Color wallColor = room.getWallColor() != null ? room.getWallColor() : new Color(75, 90, 105);
        float wallThick = 0.13f;
        g2d.setColor(wallColor);
        g2d.setStroke(new BasicStroke(wallThick, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g2d.draw(new Rectangle2D.Double(-w / 2, -d / 2, w, d));

        // ─ Inset floor area ─
        double inset = wallThick / 2.0;
        Rectangle2D.Double floor = new Rectangle2D.Double(-w / 2 + inset, -d / 2 + inset,
                                                           w - wallThick, d - wallThick);

        // Wooden floor — warm oak gradient
        Color floorBase = room.getFloorColor() != null ? room.getFloorColor() : new Color(190, 145, 85);
        Color floorShade = new Color(
                Math.max(0, floorBase.getRed() - 22),
                Math.max(0, floorBase.getGreen() - 22),
                Math.max(0, floorBase.getBlue() - 22)
        );
        GradientPaint woodGrad = new GradientPaint(
                (float)(-w / 2 + inset), 0f, floorBase,
                (float)(w / 2 - inset), 0f, floorShade);
        g2d.setPaint(woodGrad);
        g2d.fill(floor);

        // Subtle wood-grain stripes
        g2d.setColor(new Color(
                Math.max(0, floorShade.getRed() - 8),
                Math.max(0, floorShade.getGreen() - 8),
                Math.max(0, floorShade.getBlue() - 8),
                60
        ));
        float grainW = (float)(2.0 / 80.0 / zoom); // thin stripe
        g2d.setStroke(new BasicStroke(grainW));
        double grainStep = 0.18;
        for (double gy = -d / 2 + inset; gy < d / 2 - inset; gy += grainStep) {
            g2d.draw(new java.awt.geom.Line2D.Double(-w / 2 + inset, gy, w / 2 - inset, gy));
        }

        // Reset paint
        g2d.setPaint(null);

        // L-shaped cutout
        if (room.getShape() == Room.Shape.L_SHAPED) {
            g2d.setColor(new Color(75, 75, 75));
            g2d.fill(new Rectangle2D.Double(0, -d / 2, w / 2, d / 2));
            g2d.setColor(wallColor);
            g2d.setStroke(new BasicStroke(wallThick));
            g2d.draw(new Rectangle2D.Double(0, -d / 2, w / 2, d / 2));
        }

        // ─ Centre label  ─
        double area = w * d;
        String roomLabel = String.format("Room (%.3f m\u00b2)", area);
        float labelSize = 0.22f;
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 1).deriveFont(labelSize));
        g2d.setColor(new Color(150, 130, 100, 220));
        FontMetrics fm = g2d.getFontMetrics();
        float lw2 = fm.stringWidth(roomLabel);
        g2d.drawString(roomLabel, -lw2 / 2, labelSize / 2);

        // ─ Green selection outline ─
        if (roomSelected && selectedFurniture == null) {
            g2d.setColor(new Color(41, 180, 76));
            g2d.setStroke(new BasicStroke(wallThick * 0.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(new Rectangle2D.Double(-w / 2 - wallThick * 0.6f, -d / 2 - wallThick * 0.6f,
                    w + wallThick * 1.2f, d + wallThick * 1.2f));
        }

        // ─ Dimension arrows ─
        AffineTransform savedTransform = g2d.getTransform();
        g2d.setColor(new Color(50, 50, 50));
        float arrowStroke = (float)(1.5 / 80.0 / zoom);
        g2d.setStroke(new BasicStroke(arrowStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        double gap = 0.35; // gap from wall edge
        double arrowHead = 0.12;
        String wLabel = String.format("%.2f m", w);
        String dLabel = String.format("%.2f m", d);
        float dimFontSize = 0.16f;
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 1).deriveFont(dimFontSize));
        fm = g2d.getFontMetrics();

        // Top dimension (width arrow, above room)
        double arrowY = -d / 2 - gap;
        drawArrowLine(g2d, -w / 2, arrowY, w / 2, arrowY, arrowHead);
        String wTop = wLabel;
        g2d.drawString(wTop, (float)(-fm.stringWidth(wTop) / 2.0), (float)(arrowY - arrowHead * 0.3));

        // Bottom dimension (width arrow, below room)
        arrowY = d / 2 + gap;
        drawArrowLine(g2d, w / 2, arrowY, -w / 2, arrowY, arrowHead);
        g2d.drawString(wTop, (float)(-fm.stringWidth(wTop) / 2.0), (float)(arrowY + arrowHead + dimFontSize));

        // Left dimension (depth arrow, left of room)
        double arrowX = -w / 2 - gap;
        g2d.rotate(-Math.PI / 2);
        drawArrowLine(g2d, -d / 2, arrowX, d / 2, arrowX, arrowHead);
        g2d.drawString(dLabel, (float)(-fm.stringWidth(dLabel) / 2.0), (float)(arrowX - arrowHead * 0.3));
        g2d.setTransform(savedTransform);

        // Right dimension (depth arrow, right of room)
        arrowX = w / 2 + gap;
        g2d.rotate(Math.PI / 2);
        drawArrowLine(g2d, -d / 2, -arrowX, d / 2, -arrowX, arrowHead);
        g2d.drawString(dLabel, (float)(-fm.stringWidth(dLabel) / 2.0), (float)(-arrowX - arrowHead * 0.3));
        g2d.setTransform(savedTransform);
    }

    /** Draws a double-headed arrow from (x1,y1) to (x2,y2) with arrowhead size ah. */
    private void drawArrowLine(Graphics2D g2, double x1, double y1, double x2, double y2, double ah) {
        // Main line
        g2.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
        // Direction vector
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-9) return;
        dx /= len; dy /= len;
        // Arrow at (x2,y2)
        g2.draw(new java.awt.geom.Line2D.Double(x2, y2, x2 - dx * ah + dy * ah * 0.4, y2 - dy * ah - dx * ah * 0.4));
        g2.draw(new java.awt.geom.Line2D.Double(x2, y2, x2 - dx * ah - dy * ah * 0.4, y2 - dy * ah + dx * ah * 0.4));
        // Arrow at (x1,y1) — reverse direction
        g2.draw(new java.awt.geom.Line2D.Double(x1, y1, x1 + dx * ah + dy * ah * 0.4, y1 + dy * ah - dx * ah * 0.4));
        g2.draw(new java.awt.geom.Line2D.Double(x1, y1, x1 + dx * ah - dy * ah * 0.4, y1 + dy * ah + dx * ah * 0.4));
    }

    private void drawFurniture(Graphics2D g2d) {
        if (design.getFurnitureList() == null) return;

        for (Furniture f : design.getFurnitureList()) {
            drawSingleFurniture(g2d, f, f == selectedFurniture);
        }
    }

    private void drawSingleFurniture(Graphics2D g2d, Furniture f, boolean isSelected) {
        AffineTransform old = g2d.getTransform();

        // Move to furniture position and apply rotation
        g2d.translate(f.getX(), f.getY());
        g2d.rotate(Math.toRadians(f.getRotation()));

        double w = f.getWidth();
        double d = f.getDepth();

        // Draw furniture body
        Rectangle2D.Double rect = new Rectangle2D.Double(-w / 2, -d / 2, w, d);
        Color fillColor = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
        g2d.setColor(fillColor);
        g2d.fill(rect);

        // Border
        g2d.setColor(RenderUtils.darken(f.getColor(), 0.6));
        g2d.setStroke(new BasicStroke(0.03f));
        g2d.draw(rect);

        // Draw furniture type-specific details
        drawFurnitureDetails(g2d, f, w, d);

        // Selection highlight — bright green border
        if (isSelected) {
            g2d.setColor(new Color(41, 180, 76));
            g2d.setStroke(new BasicStroke(0.07f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(new Rectangle2D.Double(-w / 2 - 0.07, -d / 2 - 0.07, w + 0.14, d + 0.14));
        }

        // Label
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 1).deriveFont(0.14f));
        g2d.setColor(new Color(255, 255, 255, 200));
        FontMetrics fm = g2d.getFontMetrics();
        String label = f.getName();
        float lw = fm.stringWidth(label);
        // Background for label
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fill(new Rectangle2D.Double(-lw / 2.0 - 0.04, -0.1, lw + 0.08, 0.2));
        g2d.setColor(Color.WHITE);
        g2d.drawString(label, -lw / 2.0f, 0.05f);

        g2d.setTransform(old);
    }

    private void drawFurnitureDetails(Graphics2D g2d, Furniture f, double w, double d) {
        g2d.setColor(RenderUtils.darken(f.getColor(), 0.75));
        g2d.setStroke(new BasicStroke(0.02f));

        switch (f.getType()) {
            case CHAIR -> {
                // Seat + backrest indicator
                g2d.draw(new Rectangle2D.Double(-w * 0.35, -d * 0.35, w * 0.7, d * 0.7));
                g2d.fill(new Rectangle2D.Double(-w * 0.4, -d * 0.45, w * 0.8, d * 0.1));
            }
            case DINING_TABLE, SIDE_TABLE, COFFEE_TABLE, DESK -> {
                // Table legs
                double legSize = 0.06;
                double inset = 0.08;
                g2d.fill(new Rectangle2D.Double(-w / 2 + inset, -d / 2 + inset, legSize, legSize));
                g2d.fill(new Rectangle2D.Double(w / 2 - inset - legSize, -d / 2 + inset, legSize, legSize));
                g2d.fill(new Rectangle2D.Double(-w / 2 + inset, d / 2 - inset - legSize, legSize, legSize));
                g2d.fill(new Rectangle2D.Double(w / 2 - inset - legSize, d / 2 - inset - legSize, legSize, legSize));
            }
            case SOFA -> {
                // Arms + back
                double armW = w * 0.08;
                g2d.fill(new Rectangle2D.Double(-w / 2, -d / 2, armW, d)); // left arm
                g2d.fill(new Rectangle2D.Double(w / 2 - armW, -d / 2, armW, d)); // right arm
                g2d.fill(new Rectangle2D.Double(-w / 2, -d / 2, w, d * 0.2)); // back
                // Cushion lines
                g2d.setStroke(new BasicStroke(0.015f));
                int cushions = (int) (w / 0.6);
                for (int i = 1; i < cushions; i++) {
                    double cx = -w / 2 + armW + (w - 2 * armW) / cushions * i;
                    g2d.draw(new java.awt.geom.Line2D.Double(cx, -d * 0.3, cx, d * 0.45));
                }
            }
            case SHELF, WARDROBE -> {
                // Shelf lines
                int shelves = (int) (f.getHeight() / 0.4);
                for (int i = 1; i < shelves; i++) {
                    double sy = -d / 2 + d / shelves * i;
                    g2d.draw(new java.awt.geom.Line2D.Double(-w * 0.4, sy, w * 0.4, sy));
                }
            }
            case BED -> {
                // Pillow area
                g2d.setColor(RenderUtils.lighten(f.getColor(), 0.3));
                g2d.fill(new Rectangle2D.Double(-w * 0.35, -d * 0.4, w * 0.3, d * 0.15));
                g2d.fill(new Rectangle2D.Double(w * 0.05, -d * 0.4, w * 0.3, d * 0.15));
            }
            case LAMP -> {
                // Circle for lamp
                g2d.draw(new java.awt.geom.Ellipse2D.Double(-w * 0.3, -d * 0.3, w * 0.6, d * 0.6));
                g2d.fill(new java.awt.geom.Ellipse2D.Double(-w * 0.1, -d * 0.1, w * 0.2, d * 0.2));
            }
            case PENDANT_LIGHT, CEILING_LIGHT -> {
                // Concentric circles with glow ring
                g2d.draw(new java.awt.geom.Ellipse2D.Double(-w * 0.4, -d * 0.4, w * 0.8, d * 0.8));
                g2d.draw(new java.awt.geom.Ellipse2D.Double(-w * 0.2, -d * 0.2, w * 0.4, d * 0.4));
                if (f.isLightOn()) {
                    Color lc = f.getLightColor() != null ? f.getLightColor() : new Color(255,240,200);
                    g2d.setColor(new Color(lc.getRed(), lc.getGreen(), lc.getBlue(), 80));
                    g2d.fill(new java.awt.geom.Ellipse2D.Double(-w * 0.45, -d * 0.45, w * 0.9, d * 0.9));
                }
                g2d.setColor(RenderUtils.darken(f.getColor(), 0.75));
                g2d.fill(new java.awt.geom.Ellipse2D.Double(-w * 0.08, -d * 0.08, w * 0.16, d * 0.16));
            }
            case FLOOR_LAMP_LIGHT -> {
                g2d.draw(new java.awt.geom.Ellipse2D.Double(-w * 0.35, -d * 0.35, w * 0.7, d * 0.7));
                g2d.fill(new java.awt.geom.Ellipse2D.Double(-w * 0.06, -d * 0.06, w * 0.12, d * 0.12));
            }
            case WALL_LIGHT -> {
                g2d.draw(new java.awt.geom.Arc2D.Double(-w * 0.4, -d * 0.4, w * 0.8, d * 0.8, 0, 180, java.awt.geom.Arc2D.OPEN));
                g2d.fill(new Rectangle2D.Double(-w * 0.08, -d * 0.3, w * 0.16, d * 0.15));
            }
            case SPOTLIGHT -> {
                // Triangle beam shape
                java.awt.geom.Path2D.Double tri = new java.awt.geom.Path2D.Double();
                tri.moveTo(0, -d * 0.15);
                tri.lineTo(-w * 0.3, d * 0.4);
                tri.lineTo(w * 0.3, d * 0.4);
                tri.closePath();
                g2d.draw(tri);
                g2d.fill(new java.awt.geom.Ellipse2D.Double(-w * 0.08, -d * 0.08, w * 0.16, d * 0.16));
            }
            case TABLE_LAMP_LIGHT -> {
                g2d.draw(new java.awt.geom.Ellipse2D.Double(-w * 0.35, -d * 0.35, w * 0.7, d * 0.7));
                g2d.setStroke(new BasicStroke(0.015f));
                g2d.draw(new java.awt.geom.Line2D.Double(-w * 0.2, -d * 0.1, w * 0.2, -d * 0.1));
                g2d.draw(new java.awt.geom.Line2D.Double(-w * 0.2, d * 0.1, w * 0.2, d * 0.1));
                g2d.fill(new java.awt.geom.Ellipse2D.Double(-w * 0.06, -d * 0.06, w * 0.12, d * 0.12));
            }
        }
    }

    private void drawHUD(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        // Zoom indicator
        String zoomText = String.format("Zoom: %.0f%%", zoom * 100);
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect(8, getHeight() - 30, g2d.getFontMetrics().stringWidth(zoomText) + 16, 22, 6, 6);
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString(zoomText, 16, getHeight() - 14);

        if (design != null && roomVisible && design.getRoom() != null) {
            // Room info
            Room room = design.getRoom();
            String roomInfo = String.format("Room: %.1f × %.1f m | %s | %d items",
                    room.getWidth(), room.getDepth(), room.getShape().getDisplayName(),
                    design.getFurnitureList().size());
            int riWidth = g2d.getFontMetrics().stringWidth(roomInfo) + 16;
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRoundRect(getWidth() - riWidth - 8, getHeight() - 30, riWidth, 22, 6, 6);
            g2d.setColor(new Color(60, 60, 60));
            g2d.drawString(roomInfo, getWidth() - riWidth, getHeight() - 14);
        }
    }

    // ─── Distance measurements (drawn in room-coordinate space) ───

    private void drawDistanceMeasurements(Graphics2D g2d, Furniture f) {
        if (design == null || design.getRoom() == null) return;
        Room room = design.getRoom();
        double roomW = room.getWidth(), roomD = room.getDepth();
        double fx = f.getX(), fy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth();
        double currentScale = pixelsPerMeter * zoom;

        double minDist = 0.05;
        Color lineColor = new Color(41, 180, 76);
        float lineStroke = (float)(1.5 / currentScale);
        g2d.setStroke(new BasicStroke(lineStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(lineColor);

        // Right wall distance
        double distRight = roomW / 2.0 - (fx + fw / 2.0);
        if (distRight > minDist) {
            double x1 = fx + fw / 2.0, x2 = roomW / 2.0;
            g2d.draw(new java.awt.geom.Line2D.Double(x1, fy, x2, fy));
            drawDistancePill(g2d, currentScale, (x1 + x2) / 2.0, fy, String.format("%.2f m", distRight));
        }
        // Bottom wall distance
        double distBottom = roomD / 2.0 - (fy + fd / 2.0);
        if (distBottom > minDist) {
            double y1 = fy + fd / 2.0, y2 = roomD / 2.0;
            g2d.draw(new java.awt.geom.Line2D.Double(fx, y1, fx, y2));
            drawDistancePill(g2d, currentScale, fx, (y1 + y2) / 2.0, String.format("%.2f m", distBottom));
        }
        // Left wall distance
        double distLeft = (fx - fw / 2.0) - (-roomW / 2.0);
        if (distLeft > minDist) {
            double x1 = -roomW / 2.0, x2 = fx - fw / 2.0;
            g2d.draw(new java.awt.geom.Line2D.Double(x1, fy, x2, fy));
            drawDistancePill(g2d, currentScale, (x1 + x2) / 2.0, fy, String.format("%.2f m", distLeft));
        }
        // Top wall distance
        double distTop = (fy - fd / 2.0) - (-roomD / 2.0);
        if (distTop > minDist) {
            double y1 = -roomD / 2.0, y2 = fy - fd / 2.0;
            g2d.draw(new java.awt.geom.Line2D.Double(fx, y1, fx, y2));
            drawDistancePill(g2d, currentScale, fx, (y1 + y2) / 2.0, String.format("%.2f m", distTop));
        }
    }

    private void drawDistancePill(Graphics2D g2d, double scale, double cx, double cy, String text) {
        float fontSize = (float)(11.0 / scale);
        Font pillFont = new Font("Segoe UI", Font.BOLD, 1).deriveFont(fontSize);
        g2d.setFont(pillFont);
        FontMetrics fm = g2d.getFontMetrics();
        double tw = fm.stringWidth(text);
        double th = fm.getAscent();
        double padX = 5.0 / scale;
        double padY = 3.0 / scale;
        double pillW = tw + padX * 2;
        double pillH = th + padY * 2;
        float arc = (float)(8.0 / scale);

        g2d.setColor(new Color(41, 180, 76));
        g2d.fill(new java.awt.geom.RoundRectangle2D.Double(
                cx - pillW / 2, cy - pillH / 2, pillW, pillH, arc, arc));
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, (float)(cx - tw / 2.0), (float)(cy + th / 2.0 - fm.getDescent()));
    }

    // ─── Screen-space overlays ───

    private void drawScreenOverlays(Graphics2D g2) {
        // Rotation icon overlay hidden by request.
    }

    private void drawRotationHandle(Graphics2D g2, int cx, int cy) {
        if (rotateHandleImage != null) {
            int size = 28;
            g2.drawImage(rotateHandleImage, cx - size, cy - size, size * 2, size * 2, null);
            return;
        }

        int r = 14;
        // Green filled circle
        g2.setColor(new Color(41, 180, 76));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        // Rotation arrow inside (white arc + arrowhead)
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - 7, cy - 7, 14, 14, 45, 270);
        // arrowhead at arc end (angle = 45 + 270 = 315 degrees)
        double endRad = Math.toRadians(315);
        int ax = (int)(cx + 7 * Math.cos(endRad));
        int ay = (int)(cy - 7 * Math.sin(endRad));
        g2.drawLine(ax, ay, ax + 4, ay - 3);
        g2.drawLine(ax, ay, ax - 2, ay - 4);
    }

    private Image loadRotateHandleImage() {
        java.net.URL url = getClass().getResource("/icons/rotate.png");
        return (url != null) ? new ImageIcon(url).getImage() : null;
    }

    // Mouse event handlers
    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();

        if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && e.isAltDown())) {
            panning = true;
            panStartX = e.getX();
            panStartY = e.getY();
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            return;
        }

        if (SwingUtilities.isLeftMouseButton(e) && design != null) {
            double[] roomCoords = screenToRoom(e.getX(), e.getY());
            Furniture hit = design.findFurnitureAt(roomCoords[0], roomCoords[1]);

            if (hit != null) {
                selectedFurniture = hit;
                roomSelected = false;
                notifyBeforeModified();  // snapshot before drag starts
                dragging = true;
                dragOffsetX = roomCoords[0] - hit.getX();
                dragOffsetY = roomCoords[1] - hit.getY();
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                selectedFurniture = null;
                // Check if click is inside the room
                Room room = design.getRoom();
                if (roomVisible && room != null && Math.abs(roomCoords[0]) <= room.getWidth() / 2.0
                        && Math.abs(roomCoords[1]) <= room.getDepth() / 2.0) {
                    roomSelected = true;
                    notifyRoomSelected();
                } else {
                    roomSelected = false;
                }
            }
            notifySelectionChanged();
            repaint();
        }

        // Right-click context menu
        if (SwingUtilities.isRightMouseButton(e) && design != null) {
            double[] roomCoords = screenToRoom(e.getX(), e.getY());
            Furniture hit = design.findFurnitureAt(roomCoords[0], roomCoords[1]);
            if (hit != null) {
                selectedFurniture = hit;
                notifySelectionChanged();
                repaint();
                showContextMenu(e, hit);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragging) {
            dragging = false;
            setCursor(Cursor.getDefaultCursor());
            notifyDesignModified();
        }
        if (panning) {
            panning = false;
            setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (panning) {
            panX += e.getX() - panStartX;
            panY += e.getY() - panStartY;
            panStartX = e.getX();
            panStartY = e.getY();
            repaint();
            return;
        }

        if (dragging && selectedFurniture != null) {
            double[] roomCoords = screenToRoom(e.getX(), e.getY());
            selectedFurniture.setX(roomCoords[0] - dragOffsetX);
            selectedFurniture.setY(roomCoords[1] - dragOffsetY);
            repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
        zoom = Math.max(0.2, Math.min(5.0, zoom * factor));
        repaint();
    }

    private void showContextMenu(MouseEvent e, Furniture furniture) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem colorItem = new JMenuItem("Change Color...");
        colorItem.addActionListener(ev -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Furniture Color", furniture.getColor());
            if (newColor != null) {
                notifyBeforeModified();
                furniture.setColor(newColor);
                notifyDesignModified();
                repaint();
            }
        });

        JMenuItem rotateItem = new JMenuItem("Rotate 90°");
        rotateItem.addActionListener(ev -> {
            notifyBeforeModified();
            furniture.setRotation(furniture.getRotation() + 90);
            notifyDesignModified();
            repaint();
        });

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(ev -> {
            notifyBeforeModified();
            design.removeFurniture(furniture);
            selectedFurniture = null;
            notifySelectionChanged();
            notifyDesignModified();
            repaint();
        });

        menu.add(colorItem);
        menu.add(rotateItem);
        menu.addSeparator();
        menu.add(deleteItem);
        menu.show(this, e.getX(), e.getY());
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}

    public double getZoom() { return zoom; }
    public void setZoom(double zoom) { this.zoom = Math.max(0.2, Math.min(5.0, zoom)); repaint(); }
}
