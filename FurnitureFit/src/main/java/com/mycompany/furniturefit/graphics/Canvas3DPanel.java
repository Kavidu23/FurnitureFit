package com.mycompany.furniturefit.graphics;

import com.mycompany.furniturefit.model.Design;
import com.mycompany.furniturefit.model.Furniture;
import com.mycompany.furniturefit.model.Room;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 3D Perspective view with INFINITE rotation (no angle clamping)
 * and type-specific furniture models (chairs with legs, tables, sofas, etc.).
 */
public class Canvas3DPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

    private Design design;
    private Furniture selectedFurniture;
    private boolean roomVisible = true;
    private double zoom = 1.0;

    // Camera — NO limits on angles
    private double cameraDistance = 12.0;
    private double cameraElevation = 25.0;
    private double cameraAzimuth = 30.0;
    private double fov = 60.0;

    // Pan state
    private double panX = 0, panY = 0;
    private boolean panning = false;
    private int lastMouseX, lastMouseY;

    private Runnable onSelectionChanged;

    // Night mode
    private boolean nightMode = false;

    public Canvas3DPanel() {
        setBackground(new Color(200, 220, 240)); // sky blue-ish
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setFocusable(true);
    }

    // ─── Public API ───
    public void setDesign(Design design) { this.design = design; this.selectedFurniture = null; repaint(); }
    public void setRoomVisible(boolean roomVisible) { this.roomVisible = roomVisible; repaint(); }
    public void setSelectedFurniture(Furniture f) { this.selectedFurniture = f; repaint(); }
    public Furniture getSelectedFurniture() { return selectedFurniture; }
    public void setNightMode(boolean nightMode) { this.nightMode = nightMode; repaint(); }
    public boolean isNightMode() { return nightMode; }
    public void setOnSelectionChanged(Runnable cb) { this.onSelectionChanged = cb; }
    public void zoomIn()  { zoom = Math.min(5.0, zoom * 1.15); repaint(); }
    public void zoomOut() { zoom = Math.max(0.2, zoom * 0.87); repaint(); }
    public void setRotationAngle(double a) { cameraAzimuth = a; repaint(); }
    public double getRotationAngle() { return cameraAzimuth; }

    // ─── Perspective helpers ───

    private Point2D.Double perspProject(double wx, double wy, double wz) {
        double azRad = Math.toRadians(cameraAzimuth);
        double elRad = Math.toRadians(cameraElevation);
        double cosA = Math.cos(azRad), sinA = Math.sin(azRad);
        double cosE = Math.cos(elRad), sinE = Math.sin(elRad);

        double rx =  wx * cosA + wy * sinA;
        double ry = -wx * sinA * sinE + wy * cosA * sinE + wz * cosE;
        double rz = -wx * sinA * cosE + wy * cosA * cosE - wz * sinE;
        rz += cameraDistance;

        double fovScale = 1.0 / Math.tan(Math.toRadians(fov / 2.0));
        double panelH = getHeight() > 0 ? getHeight() : 700;
        double halfH = panelH / 2.0;
        if (rz < 0.1) rz = 0.1;

        double sx = (rx * fovScale / rz) * halfH * zoom + getWidth() / 2.0 + panX;
        double sy = -(ry * fovScale / rz) * halfH * zoom + getHeight() / 2.0 + panY;
        return new Point2D.Double(sx, sy);
    }

    private double perspDepth(double wx, double wy, double wz) {
        double azRad = Math.toRadians(cameraAzimuth);
        double elRad = Math.toRadians(cameraElevation);
        return -wx * Math.sin(azRad) * Math.cos(elRad) +
                wy * Math.cos(azRad) * Math.cos(elRad) -
                wz * Math.sin(elRad) + cameraDistance;
    }

    // ─── Painting ───

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sky gradient background
        Color skyTop, skyBot, groundTop, groundBot;
        if (nightMode) {
            skyTop = new Color(10, 15, 40);
            skyBot = new Color(25, 30, 60);
            groundTop = new Color(30, 32, 38);
            groundBot = new Color(22, 24, 30);
        } else {
            skyTop = new Color(170, 210, 240);
            skyBot = new Color(232, 242, 250);
            groundTop = new Color(225, 228, 232);
            groundBot = new Color(210, 215, 220);
        }
        GradientPaint sky = new GradientPaint(0, 0, skyTop,
                0, (int)(getHeight() * 0.55), skyBot);
        g2.setPaint(sky);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Ground plane
        GradientPaint ground = new GradientPaint(0, (int)(getHeight() * 0.55), groundTop,
                0, getHeight(), groundBot);
        g2.setPaint(ground);
        g2.fillRect(0, (int)(getHeight() * 0.55), getWidth(), getHeight());

        if (design == null) {
            g2.setColor(new Color(100, 100, 100));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            String msg = "3D View — Create a design to visualize";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            g2.dispose();
            return;
        }

        if (roomVisible) {
            drawRoom3D(g2);
            drawFurniture3D(g2);
        }

        // Draw lamp glow effects in night mode
        if (nightMode && design != null && roomVisible) {
            drawLampGlows(g2);
        }

        g2.dispose();
        drawHUD(g);
    }

    // ─── Room ───

    private void drawRoom3D(Graphics2D g2) {
        Room room = design.getRoom();
        double w = room.getWidth(), d = room.getDepth(), h = room.getHeight();

        // Night mode darkening factor
        double nightDim = nightMode ? 0.25 : 1.0;

        // Floor
        Color floorCol = nightMode ? RenderUtils.darken(room.getFloorColor(), nightDim) : room.getFloorColor();
        fillQuad(g2, floorCol,
                -w/2, -d/2, 0,  w/2, -d/2, 0,  w/2, d/2, 0,  -w/2, d/2, 0);
        // Floor grid
        g2.setColor(RenderUtils.darken(floorCol, 0.85));
        g2.setStroke(new BasicStroke(0.5f));
        for (double x = -w/2; x <= w/2; x += 0.5) {
            drawLine3D(g2, x, -d/2, 0, x, d/2, 0);
        }
        for (double y = -d/2; y <= d/2; y += 0.5) {
            drawLine3D(g2, -w/2, y, 0, w/2, y, 0);
        }

        // Walls — draw all 4, using normal-dot-product to decide if facing camera
        Color wallCol = nightMode ? RenderUtils.darken(room.getWallColor(), nightDim) : room.getWallColor();
        // Back wall (y = -d/2), normal = (0, -1, 0)
        drawWallIfVisible(g2, wallCol, 0, -1, 0,
                -w/2, -d/2, 0,  w/2, -d/2, 0,  w/2, -d/2, h,  -w/2, -d/2, h, 0.85);
        // Front wall (y = d/2), normal = (0, 1, 0)
        drawWallIfVisible(g2, wallCol, 0, 1, 0,
                w/2, d/2, 0,  -w/2, d/2, 0,  -w/2, d/2, h,  w/2, d/2, h, 0.80);
        // Left wall (x = -w/2), normal = (-1, 0, 0)
        drawWallIfVisible(g2, wallCol, -1, 0, 0,
                -w/2, d/2, 0,  -w/2, -d/2, 0,  -w/2, -d/2, h,  -w/2, d/2, h, 0.75);
        // Right wall (x = w/2), normal = (1, 0, 0)
        drawWallIfVisible(g2, wallCol, 1, 0, 0,
                w/2, -d/2, 0,  w/2, d/2, 0,  w/2, d/2, h,  w/2, -d/2, h, 0.70);
    }

    /** Draw wall quad if its outward normal faces the camera. */
    private void drawWallIfVisible(Graphics2D g2, Color base,
                                   double nx, double ny, double nz,
                                   double x1, double y1, double z1,
                                   double x2, double y2, double z2,
                                   double x3, double y3, double z3,
                                   double x4, double y4, double z4,
                                   double darkenFactor) {
        // Camera direction (un-rotated view vector)
        double azRad = Math.toRadians(cameraAzimuth);
        double elRad = Math.toRadians(cameraElevation);
        double camX = Math.sin(azRad) * Math.cos(elRad);
        double camY = -Math.cos(azRad) * Math.cos(elRad);
        double camZ = Math.sin(elRad);
        double dot = nx * camX + ny * camY + nz * camZ;
        if (dot > -0.01) return; // wall faces away — skip

        Color fill = RenderUtils.darken(base, darkenFactor);
        fillQuad(g2, fill, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
        g2.setColor(RenderUtils.darken(base, darkenFactor * 0.7));
        g2.setStroke(new BasicStroke(1.5f));
        strokeQuad(g2, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
    }

    // ─── Furniture ───

    private void drawFurniture3D(Graphics2D g2) {
        List<Furniture> sorted = new ArrayList<>(design.getFurnitureList());
        sorted.sort(Comparator.comparingDouble(f ->
                -perspDepth(f.getX(), f.getY(), f.getHeight() / 2.0)));

        for (Furniture f : sorted) {
            drawFurnitureTyped(g2, f, f == selectedFurniture);
        }
    }

    private void drawFurnitureTyped(Graphics2D g2, Furniture f, boolean selected) {
        switch (f.getType()) {
            case CHAIR        -> drawChair(g2, f, selected);
            case DINING_TABLE -> drawTable(g2, f, selected);
            case SIDE_TABLE   -> drawTable(g2, f, selected);
            case COFFEE_TABLE -> drawTable(g2, f, selected);
            case SOFA         -> drawSofa(g2, f, selected);
            case BED          -> drawBed(g2, f, selected);
            case SHELF        -> drawShelf(g2, f, selected);
            case WARDROBE     -> drawWardrobe(g2, f, selected);
            case DESK         -> drawDesk(g2, f, selected);
            case LAMP         -> drawLamp(g2, f, selected);
        case PENDANT_LIGHT, FLOOR_LAMP_LIGHT, CEILING_LIGHT,
             WALL_LIGHT, SPOTLIGHT, TABLE_LAMP_LIGHT
                          -> drawLamp(g2, f, selected); // light fixtures reuse lamp renderer
        }
        // Label
        drawFurnitureLabel(g2, f);
    }

    // ── CHAIR: 4 legs + seat + backrest ──
    private void drawChair(Graphics2D g2, Furniture f, boolean sel) {
        double cx = f.getX(), cy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth(), fh = f.getHeight();
        Color col = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
        double legW = Math.min(fw, fd) * 0.10;
        double seatH = fh * 0.47;
        double seatThick = fh * 0.05;

        // 4 legs (tapered look via slight inset)
        double inset = legW * 0.3;
        double lx1 = cx - fw/2 + legW/2 + inset, lx2 = cx + fw/2 - legW/2 - inset;
        double ly1 = cy - fd/2 + legW/2 + inset, ly2 = cy + fd/2 - legW/2 - inset;
        Color legCol = RenderUtils.darken(col, 0.85);
        drawBox(g2, legCol, lx1 - legW/2, ly1 - legW/2, 0, legW, legW, seatH, sel);
        drawBox(g2, legCol, lx2 - legW/2, ly1 - legW/2, 0, legW, legW, seatH, sel);
        drawBox(g2, legCol, lx1 - legW/2, ly2 - legW/2, 0, legW, legW, seatH, sel);
        drawBox(g2, legCol, lx2 - legW/2, ly2 - legW/2, 0, legW, legW, seatH, sel);

        // Seat (slightly padded look)
        drawBox(g2, RenderUtils.lighten(col, 0.08), cx - fw/2, cy - fd/2, seatH, fw, fd, seatThick, sel);
        // Seat cushion (slightly inset, softer color)
        double cushInset = fw * 0.05;
        drawBox(g2, RenderUtils.lighten(col, 0.15), cx - fw/2 + cushInset, cy - fd/2 + cushInset,
                seatH + seatThick * 0.3, fw - 2*cushInset, fd - 2*cushInset, seatThick * 0.7, sel);

        // Backrest (curved appearance via two overlapping boxes)
        double backH = fh - seatH - seatThick;
        double backThick = legW * 1.3;
        drawBox(g2, col, cx - fw/2, cy - fd/2, seatH + seatThick, fw, backThick, backH, sel);
        // Backrest cap (slightly lighter)
        drawBox(g2, RenderUtils.lighten(col, 0.1), cx - fw/2, cy - fd/2, seatH + seatThick + backH * 0.85,
                fw, backThick * 1.1, backH * 0.15, sel);
    }

    // ── TABLE: 4 legs + flat top ──
    private void drawTable(Graphics2D g2, Furniture f, boolean sel) {
        double cx = f.getX(), cy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth(), fh = f.getHeight();
        Color col = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
        double legW = Math.min(fw, fd) * 0.07;
        double topThick = fh * 0.06;
        double legH = fh - topThick;

        double legInset = legW * 1.5;
        double lx1 = cx - fw/2 + legInset, lx2 = cx + fw/2 - legInset;
        double ly1 = cy - fd/2 + legInset, ly2 = cy + fd/2 - legInset;
        Color legCol = RenderUtils.darken(col, 0.8);
        drawBox(g2, legCol, lx1 - legW/2, ly1 - legW/2, 0, legW, legW, legH, sel);
        drawBox(g2, legCol, lx2 - legW/2, ly1 - legW/2, 0, legW, legW, legH, sel);
        drawBox(g2, legCol, lx1 - legW/2, ly2 - legW/2, 0, legW, legW, legH, sel);
        drawBox(g2, legCol, lx2 - legW/2, ly2 - legW/2, 0, legW, legW, legH, sel);

        // Support bar under table (realism)
        double barH = legW * 0.8;
        drawBox(g2, RenderUtils.darken(col, 0.75), lx1 - legW/2, cy - barH/2, legH * 0.3, lx2 - lx1 + legW, barH, barH, sel);

        // Top surface with slight overhang
        double overhang = 0.02;
        drawBox(g2, RenderUtils.lighten(col, 0.12), cx - fw/2 - overhang, cy - fd/2 - overhang, legH,
                fw + 2*overhang, fd + 2*overhang, topThick, sel);
        // Wood grain line on top
        drawBox(g2, RenderUtils.lighten(col, 0.2), cx - fw * 0.35, cy - fd/2 - overhang, legH + topThick * 0.01,
                fw * 0.02, fd + 2*overhang, topThick * 0.3, sel);
    }

    // ── SOFA: base + back cushion + 2 arm rests + seat cushions ──
    private void drawSofa(Graphics2D g2, Furniture f, boolean sel) {
        double cx = f.getX(), cy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth(), fh = f.getHeight();
        Color col = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
        double baseH = fh * 0.40;
        double armW = fw * 0.08;
        double backThick = fd * 0.22;
        double cushionH = fh * 0.12;

        // Base frame (slightly darker)
        drawBox(g2, RenderUtils.darken(col, 0.7), cx - fw/2, cy - fd/2, 0, fw, fd, baseH * 0.3, sel);
        // Seat base
        drawBox(g2, col, cx - fw/2, cy - fd/2, baseH * 0.3, fw, fd, baseH * 0.7, sel);
        // Seat cushions (divided into sections)
        int cushionCount = Math.max(2, (int)(fw / 0.6));
        double cushionW = (fw - 2 * armW) / cushionCount;
        for (int i = 0; i < cushionCount; i++) {
            double gapX = 0.01;
            drawBox(g2, RenderUtils.lighten(col, 0.08),
                    cx - fw/2 + armW + i * cushionW + gapX, cy - fd/2 + backThick + 0.02,
                    baseH, cushionW - 2*gapX, fd - backThick - armW * 0.5, cushionH, sel);
        }
        // Back cushion
        drawBox(g2, RenderUtils.darken(col, 0.88), cx - fw/2 + armW * 0.5, cy - fd/2, baseH,
                fw - armW, backThick, fh - baseH, sel);
        // Back cushion pillows (individual)
        for (int i = 0; i < cushionCount; i++) {
            double gapX = 0.02;
            drawBox(g2, RenderUtils.lighten(col, 0.05),
                    cx - fw/2 + armW + i * cushionW + gapX, cy - fd/2 + 0.02,
                    baseH + cushionH, cushionW - 2*gapX, backThick * 0.7, (fh - baseH - cushionH) * 0.85, sel);
        }
        // Left arm
        drawBox(g2, RenderUtils.darken(col, 0.92), cx - fw/2, cy - fd/2, baseH * 0.3, armW, fd, fh * 0.35, sel);
        // Right arm
        drawBox(g2, RenderUtils.darken(col, 0.92), cx + fw/2 - armW, cy - fd/2, baseH * 0.3, armW, fd, fh * 0.35, sel);
        // Arm top padding
        drawBox(g2, RenderUtils.lighten(col, 0.05), cx - fw/2, cy - fd/2, baseH * 0.3 + fh * 0.35,
                armW, fd, fh * 0.04, sel);
        drawBox(g2, RenderUtils.lighten(col, 0.05), cx + fw/2 - armW, cy - fd/2, baseH * 0.3 + fh * 0.35,
                armW, fd, fh * 0.04, sel);
    }

    // ── BED: frame + mattress + headboard + pillows + blanket ──
    private void drawBed(Graphics2D g2, Furniture f, boolean sel) {
        double cx = f.getX(), cy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth(), fh = f.getHeight();
        Color col = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
        double frameH = fh * 0.30;
        double mattressH = fh * 0.28;
        double headH = fh * 0.75;
        double headThick = fd * 0.06;
        double footThick = fd * 0.04;

        // Bed legs (4 short legs)
        double legW = 0.06;
        double legH = frameH * 0.3;
        Color legCol = RenderUtils.darken(col, 0.55);
        drawBox(g2, legCol, cx - fw/2, cy - fd/2, 0, legW, legW, legH, sel);
        drawBox(g2, legCol, cx + fw/2 - legW, cy - fd/2, 0, legW, legW, legH, sel);
        drawBox(g2, legCol, cx - fw/2, cy + fd/2 - legW, 0, legW, legW, legH, sel);
        drawBox(g2, legCol, cx + fw/2 - legW, cy + fd/2 - legW, 0, legW, legW, legH, sel);

        // Frame
        drawBox(g2, RenderUtils.darken(col, 0.65), cx - fw/2, cy - fd/2, legH, fw, fd, frameH - legH, sel);
        // Frame side rails
        drawBox(g2, RenderUtils.darken(col, 0.6), cx - fw/2, cy - fd/2, legH, fw, fd * 0.04, frameH - legH + 0.02, sel);
        drawBox(g2, RenderUtils.darken(col, 0.6), cx - fw/2, cy + fd/2 - fd * 0.04, legH, fw, fd * 0.04, frameH - legH + 0.02, sel);

        // Mattress (white with slight tint)
        Color mattressCol = new Color(248, 246, 240);
        drawBox(g2, mattressCol, cx - fw/2 + 0.03, cy - fd/2 + 0.03, frameH,
                fw - 0.06, fd - 0.06, mattressH, sel);
        // Mattress quilting line
        drawBox(g2, RenderUtils.darken(mattressCol, 0.95), cx - fw/2 + 0.03, cy, frameH + mattressH * 0.5,
                fw - 0.06, 0.01, mattressH * 0.5, sel);

        // Headboard (decorative)
        drawBox(g2, col, cx - fw/2, cy - fd/2, 0, fw, headThick, headH, sel);
        // Headboard panel detail
        drawBox(g2, RenderUtils.lighten(col, 0.08), cx - fw/2 + fw * 0.1, cy - fd/2, headH * 0.15,
                fw * 0.8, headThick * 0.5, headH * 0.65, sel);

        // Footboard (shorter)
        drawBox(g2, col, cx - fw/2, cy + fd/2 - footThick, 0, fw, footThick, frameH + mattressH * 0.5, sel);

        // Pillows (rounded appearance)
        Color pillowCol = new Color(245, 245, 238);
        double pillowW = fw * 0.22, pillowD = fd * 0.13, pillowH = fh * 0.09;
        drawBox(g2, pillowCol, cx - fw * 0.32, cy - fd/2 + headThick + 0.02, frameH + mattressH,
                pillowW, pillowD, pillowH, sel);
        drawBox(g2, pillowCol, cx + fw * 0.10, cy - fd/2 + headThick + 0.02, frameH + mattressH,
                pillowW, pillowD, pillowH, sel);

        // Blanket/duvet (folded at top)
        Color blanketCol = RenderUtils.lighten(col, 0.25);
        drawBox(g2, blanketCol, cx - fw/2 + 0.04, cy - fd/2 + headThick + pillowD + 0.06, frameH + mattressH,
                fw - 0.08, fd - headThick - pillowD - footThick - 0.08, fh * 0.04, sel);
    }

    // ── SHELF: frame with horizontal shelves + books ──
    private void drawShelf(Graphics2D g2, Furniture f, boolean sel) {
        double cx = f.getX(), cy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth(), fh = f.getHeight();
        Color col = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
        double sideW = fw * 0.05;
        double shelfThick = fh * 0.025;
        int nShelves = 4;

        // Left side
        drawBox(g2, col, cx - fw/2, cy - fd/2, 0, sideW, fd, fh, sel);
        // Right side
        drawBox(g2, col, cx + fw/2 - sideW, cy - fd/2, 0, sideW, fd, fh, sel);
        // Back panel (thin)
        drawBox(g2, RenderUtils.darken(col, 0.75), cx - fw/2, cy - fd/2, 0, fw, fd * 0.04, fh, sel);
        // Shelves
        for (int i = 0; i <= nShelves; i++) {
            double sz = (fh / nShelves) * i;
            drawBox(g2, RenderUtils.lighten(col, 0.08), cx - fw/2 + sideW, cy - fd/2, sz,
                    fw - 2 * sideW, fd, shelfThick, sel);
        }
        // Decorative items on shelves (small colored boxes as books)
        Color[] bookColors = {new Color(180, 50, 50), new Color(50, 100, 150), new Color(80, 140, 60), new Color(180, 140, 50)};
        for (int i = 1; i < nShelves; i++) {
            double shelfZ = (fh / nShelves) * i + shelfThick;
            double shelfH = (fh / nShelves) - shelfThick;
            for (int j = 0; j < 2 + i % 2; j++) {
                double bookX = cx - fw/2 + sideW + 0.03 + j * fw * 0.12;
                double bookW = fw * 0.08;
                drawBox(g2, bookColors[j % bookColors.length], bookX, cy - fd/2 + fd * 0.05, shelfZ,
                        bookW, fd * 0.7, shelfH * 0.8, false);
            }
        }
    }

    // ── WARDROBE: tall box with doors, handles + top crown ──
    private void drawWardrobe(Graphics2D g2, Furniture f, boolean sel) {
        double cx = f.getX(), cy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth(), fh = f.getHeight();
        Color col = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());

        // Base plinth
        drawBox(g2, RenderUtils.darken(col, 0.65), cx - fw/2 - 0.01, cy - fd/2 - 0.01, 0,
                fw + 0.02, fd + 0.02, fh * 0.04, sel);
        // Main body
        drawBox(g2, col, cx - fw/2, cy - fd/2, fh * 0.04, fw, fd, fh * 0.9, sel);
        // Top crown molding
        drawBox(g2, RenderUtils.lighten(col, 0.08), cx - fw/2 - 0.02, cy - fd/2 - 0.02, fh * 0.94,
                fw + 0.04, fd + 0.04, fh * 0.06, sel);

        // Door panels (left and right)
        double doorGap = 0.015;
        Color doorCol = RenderUtils.lighten(col, 0.06);
        drawBox(g2, doorCol, cx - fw/2 + doorGap, cy + fd/2 - 0.01, fh * 0.06,
                fw/2 - doorGap * 1.5, 0.012, fh * 0.86, sel);
        drawBox(g2, doorCol, cx + doorGap/2, cy + fd/2 - 0.01, fh * 0.06,
                fw/2 - doorGap * 1.5, 0.012, fh * 0.86, sel);

        // Door line (center division)
        g2.setColor(RenderUtils.darken(col, 0.5));
        g2.setStroke(new BasicStroke(1.5f));
        drawLine3D(g2, cx, cy + fd/2 + 0.005, fh * 0.06, cx, cy + fd/2 + 0.005, fh * 0.92);

        // Handles (small protruding knobs)
        double knobZ = fh * 0.5;
        Color knobCol = new Color(180, 170, 150);
        drawBox(g2, knobCol, cx - 0.04, cy + fd/2 + 0.01, knobZ, 0.025, 0.035, 0.025, false);
        drawBox(g2, knobCol, cx + 0.015, cy + fd/2 + 0.01, knobZ, 0.025, 0.035, 0.025, false);
    }

    // ── DESK: table with back panel + drawer ──
    private void drawDesk(Graphics2D g2, Furniture f, boolean sel) {
        drawTable(g2, f, sel);
        double cx = f.getX(), cy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth(), fh = f.getHeight();
        Color col = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
        // Modesty panel (back panel under desk)
        drawBox(g2, RenderUtils.darken(col, 0.82), cx - fw/2, cy - fd/2, 0, fw, fd * 0.04, fh * 0.6, sel);
        // Drawer unit on the right side
        double drawerW = fw * 0.35;
        double drawerD = fd * 0.85;
        double drawerH = fh * 0.55;
        drawBox(g2, RenderUtils.darken(col, 0.78), cx + fw/2 - drawerW - 0.02, cy - fd/2 + (fd - drawerD)/2,
                0, drawerW, drawerD, drawerH, sel);
        // Drawer handle
        Color handleCol = new Color(170, 160, 140);
        drawBox(g2, handleCol, cx + fw/2 - drawerW/2 - 0.03, cy + fd/2 - 0.01,
                drawerH * 0.4, 0.06, 0.02, 0.015, false);
        drawBox(g2, handleCol, cx + fw/2 - drawerW/2 - 0.03, cy + fd/2 - 0.01,
                drawerH * 0.7, 0.06, 0.02, 0.015, false);
    }

    // ── LAMP: base + pole + shade with glow effect ──
    private void drawLamp(Graphics2D g2, Furniture f, boolean sel) {
        double cx = f.getX(), cy = f.getY();
        double fw = f.getWidth(), fd = f.getDepth(), fh = f.getHeight();
        Color col = RenderUtils.applyShadeAndBrightness(f.getColor(), f.getShadeIntensity(), f.getBrightness());
        double poleW = fw * 0.08;
        double baseH = fh * 0.04;
        double shadeH = fh * 0.22;
        double shadeW = fw * 0.75;
        double shadeTopW = fw * 0.55;

        // Base (circular, dark)
        drawBox(g2, RenderUtils.darken(col, 0.5), cx - fw * 0.25, cy - fd * 0.25, 0,
                fw * 0.5, fd * 0.5, baseH, sel);
        // Base disc detail
        drawBox(g2, RenderUtils.darken(col, 0.45), cx - fw * 0.2, cy - fd * 0.2, baseH,
                fw * 0.4, fd * 0.4, baseH * 0.3, sel);

        // Pole (thin metallic)
        Color poleCol = RenderUtils.darken(col, 0.4);
        drawBox(g2, poleCol, cx - poleW/2, cy - poleW/2, baseH * 1.3,
                poleW, poleW, fh - baseH * 1.3 - shadeH, sel);

        // Shade (wider at bottom, narrower at top - approximated with two boxes)
        Color shadeCol = new Color(255, 248, 225); // warm cream
        drawBox(g2, shadeCol, cx - shadeW/2, cy - shadeW/2, fh - shadeH,
                shadeW, shadeW, shadeH * 0.5, sel);
        drawBox(g2, RenderUtils.lighten(shadeCol, 0.05), cx - shadeTopW/2, cy - shadeTopW/2,
                fh - shadeH * 0.5, shadeTopW, shadeTopW, shadeH * 0.5, sel);

        // Light glow (semi-transparent bright box under shade)
        drawBox(g2, new Color(255, 250, 200, 60), cx - shadeW * 0.3, cy - shadeW * 0.3,
                fh - shadeH - 0.05, shadeW * 0.6, shadeW * 0.6, 0.05, false);
    }

    // ─── Generic 3D box drawing ───

    private void drawBox(Graphics2D g2, Color color, double bx, double by, double bz,
                         double bw, double bd, double bh, boolean selected) {
        // Camera direction for face culling
        double azRad = Math.toRadians(cameraAzimuth);
        double elRad = Math.toRadians(cameraElevation);
        double camDx = Math.sin(azRad) * Math.cos(elRad);
        double camDy = -Math.cos(azRad) * Math.cos(elRad);
        double camDz = Math.sin(elRad);

        g2.setStroke(new BasicStroke(1));

        // Top face (normal 0,0,1) — visible if camera looks down
        if (camDz > 0) {
            fillQuad(g2, RenderUtils.lighten(color, 0.15),
                    bx, by, bz + bh, bx + bw, by, bz + bh,
                    bx + bw, by + bd, bz + bh, bx, by + bd, bz + bh);
            g2.setColor(RenderUtils.darken(color, 0.7));
            strokeQuad(g2, bx, by, bz + bh, bx + bw, by, bz + bh,
                    bx + bw, by + bd, bz + bh, bx, by + bd, bz + bh);
        }
        // Bottom (normal 0,0,-1)
        if (camDz < 0) {
            fillQuad(g2, RenderUtils.darken(color, 0.6),
                    bx, by + bd, bz, bx + bw, by + bd, bz,
                    bx + bw, by, bz, bx, by, bz);
        }

        // Front (normal 0,1,0 — face at y+bd)
        if (camDy < 0) {
            fillQuad(g2, RenderUtils.darken(color, 0.75),
                    bx, by + bd, bz, bx + bw, by + bd, bz,
                    bx + bw, by + bd, bz + bh, bx, by + bd, bz + bh);
            g2.setColor(RenderUtils.darken(color, 0.55));
            strokeQuad(g2, bx, by + bd, bz, bx + bw, by + bd, bz,
                    bx + bw, by + bd, bz + bh, bx, by + bd, bz + bh);
        }
        // Back (normal 0,-1,0)
        if (camDy > 0) {
            fillQuad(g2, RenderUtils.darken(color, 0.70),
                    bx + bw, by, bz, bx, by, bz,
                    bx, by, bz + bh, bx + bw, by, bz + bh);
            g2.setColor(RenderUtils.darken(color, 0.5));
            strokeQuad(g2, bx + bw, by, bz, bx, by, bz,
                    bx, by, bz + bh, bx + bw, by, bz + bh);
        }

        // Right (normal 1,0,0)
        if (camDx < 0) {
            fillQuad(g2, RenderUtils.darken(color, 0.82),
                    bx + bw, by, bz, bx + bw, by + bd, bz,
                    bx + bw, by + bd, bz + bh, bx + bw, by, bz + bh);
            g2.setColor(RenderUtils.darken(color, 0.6));
            strokeQuad(g2, bx + bw, by, bz, bx + bw, by + bd, bz,
                    bx + bw, by + bd, bz + bh, bx + bw, by, bz + bh);
        }
        // Left (normal -1,0,0)
        if (camDx > 0) {
            fillQuad(g2, RenderUtils.darken(color, 0.78),
                    bx, by + bd, bz, bx, by, bz,
                    bx, by, bz + bh, bx, by + bd, bz + bh);
            g2.setColor(RenderUtils.darken(color, 0.55));
            strokeQuad(g2, bx, by + bd, bz, bx, by, bz,
                    bx, by, bz + bh, bx, by + bd, bz + bh);
        }

        // Selection highlight
        if (selected) {
            g2.setColor(new Color(41, 128, 185, 100));
            g2.setStroke(new BasicStroke(2.5f));
            strokeQuad(g2, bx, by, bz + bh, bx + bw, by, bz + bh,
                    bx + bw, by + bd, bz + bh, bx, by + bd, bz + bh);
        }
    }

    private void drawFurnitureLabel(Graphics2D g2, Furniture f) {
        Point2D.Double c = perspProject(f.getX(), f.getY(), f.getHeight());
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        String label = f.getName();
        int tw = fm.stringWidth(label);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect((int) c.x - tw / 2 - 3, (int) c.y - fm.getAscent() - 2, tw + 6, fm.getHeight() + 4, 4, 4);
        g2.setColor(Color.WHITE);
        g2.drawString(label, (int) c.x - tw / 2, (int) c.y);
    }

    // ─── Lamp glow effects (night mode) ───

    private void drawLampGlows(Graphics2D g2) {
        Composite origComp = g2.getComposite();
        for (Furniture f : design.getFurnitureList()) {
            if (f.getType() == Furniture.Type.LAMP && f.isLightOn()) {
                // Project lamp top position to screen
                Point2D.Double center = perspProject(f.getX(), f.getY(), f.getHeight() * 0.75);
                // Ground glow (projected circle on floor)
                Point2D.Double groundCenter = perspProject(f.getX(), f.getY(), 0);

                // Radial glow around the lamp shade
                int radius = (int)(120 * zoom);
                for (int i = radius; i > 0; i -= 3) {
                    float alpha = 0.04f * (1.0f - (float)i / radius);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.setColor(new Color(255, 240, 180));
                    g2.fillOval((int)center.x - i, (int)center.y - i, i * 2, i * 2);
                }

                // Ground light pool
                int groundRadius = (int)(180 * zoom);
                for (int i = groundRadius; i > 0; i -= 4) {
                    float alpha = 0.025f * (1.0f - (float)i / groundRadius);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.setColor(new Color(255, 230, 160));
                    g2.fillOval((int)groundCenter.x - i, (int)groundCenter.y - i / 2,
                            i * 2, i);
                }
            }
        }
        g2.setComposite(origComp);
    }

    // ─── Quad helpers ───

    private void fillQuad(Graphics2D g2, Color col,
                          double x1, double y1, double z1, double x2, double y2, double z2,
                          double x3, double y3, double z3, double x4, double y4, double z4) {
        Point2D.Double p1 = perspProject(x1, y1, z1), p2 = perspProject(x2, y2, z2);
        Point2D.Double p3 = perspProject(x3, y3, z3), p4 = perspProject(x4, y4, z4);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.lineTo(p4.x, p4.y);
        path.closePath();
        g2.setColor(col);
        g2.fill(path);
    }

    private void strokeQuad(Graphics2D g2,
                            double x1, double y1, double z1, double x2, double y2, double z2,
                            double x3, double y3, double z3, double x4, double y4, double z4) {
        Point2D.Double p1 = perspProject(x1, y1, z1), p2 = perspProject(x2, y2, z2);
        Point2D.Double p3 = perspProject(x3, y3, z3), p4 = perspProject(x4, y4, z4);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.lineTo(p4.x, p4.y);
        path.closePath();
        g2.draw(path);
    }

    private void drawLine3D(Graphics2D g2, double x1, double y1, double z1,
                            double x2, double y2, double z2) {
        Point2D.Double p1 = perspProject(x1, y1, z1), p2 = perspProject(x2, y2, z2);
        g2.draw(new java.awt.geom.Line2D.Double(p1.x, p1.y, p2.x, p2.y));
    }

    // ─── HUD ───

    private void drawHUD(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        String info = String.format("3D | Zoom %.0f%% | Az %.0f\u00B0 | El %.0f\u00B0",
                zoom * 100, cameraAzimuth % 360, cameraElevation);
        int w = g2.getFontMetrics().stringWidth(info) + 16;
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(8, getHeight() - 30, w, 22, 6, 6);
        g2.setColor(new Color(220, 220, 220));
        g2.drawString(info, 16, getHeight() - 14);
    }

    // ─── Mouse: INFINITE rotation ───

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        if (SwingUtilities.isMiddleMouseButton(e)) {
            panning = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        panning = false;
        setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int dx = e.getX() - lastMouseX;
        int dy = e.getY() - lastMouseY;
        lastMouseX = e.getX();
        lastMouseY = e.getY();

        if (panning) {
            panX += dx;
            panY += dy;
        } else if (SwingUtilities.isLeftMouseButton(e)) {
            // *** INFINITE rotation — no Math.max/min clamping ***
            cameraAzimuth  += dx * 0.35;
            cameraElevation += dy * 0.35;
        }
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoom *= e.getWheelRotation() < 0 ? 1.1 : 0.9;
        zoom = Math.max(0.2, Math.min(5.0, zoom));
        repaint();
    }

    @Override public void mouseClicked(MouseEvent e)  {}
    @Override public void mouseEntered(MouseEvent e)   {}
    @Override public void mouseExited(MouseEvent e)    {}
    @Override public void mouseMoved(MouseEvent e)     {}
}
