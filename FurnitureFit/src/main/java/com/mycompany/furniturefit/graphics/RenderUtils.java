package com.mycompany.furniturefit.graphics;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Shared rendering utilities for 2D and 3D views.
 */
public class RenderUtils {

    // Isometric angles
    public static final double ISO_ANGLE = Math.toRadians(30);
    public static final double COS_ISO = Math.cos(ISO_ANGLE);
    public static final double SIN_ISO = Math.sin(ISO_ANGLE);

    /**
     * Convert 3D coordinates to 2D isometric screen coordinates.
     */
    public static Point2D.Double toIsometric(double x, double y, double z) {
        double screenX = (x - y) * COS_ISO;
        double screenY = (x + y) * SIN_ISO - z;
        return new Point2D.Double(screenX, screenY);
    }

    /**
     * Darken a color by a factor (0.0 = black, 1.0 = unchanged).
     */
    public static Color darken(Color color, double factor) {
        factor = Math.max(0, Math.min(1, factor));
        return new Color(
                (int) (color.getRed() * factor),
                (int) (color.getGreen() * factor),
                (int) (color.getBlue() * factor),
                color.getAlpha()
        );
    }

    /**
     * Lighten a color by a factor (0.0 = unchanged, 1.0 = white).
     */
    public static Color lighten(Color color, double factor) {
        factor = Math.max(0, Math.min(1, factor));
        return new Color(
                (int) (color.getRed() + (255 - color.getRed()) * factor),
                (int) (color.getGreen() + (255 - color.getGreen()) * factor),
                (int) (color.getBlue() + (255 - color.getBlue()) * factor),
                color.getAlpha()
        );
    }

    /**
     * Apply shade to a color based on intensity (0 = none, 1 = full shade).
     */
    public static Color applyShade(Color color, double intensity) {
        return darken(color, 1.0 - intensity * 0.6);
    }

    /**
     * Apply brightness adjustment to a color.
     * @param color the base color
     * @param brightness -1.0 (fully dark) to +1.0 (fully light), 0 = no change
     */
    public static Color applyBrightness(Color color, double brightness) {
        brightness = Math.max(-1.0, Math.min(1.0, brightness));
        if (brightness > 0) {
            return lighten(color, brightness);
        } else if (brightness < 0) {
            return darken(color, 1.0 + brightness);
        }
        return color;
    }

    /**
     * Apply both shade and brightness to a color.
     */
    public static Color applyShadeAndBrightness(Color color, double shadeIntensity, double brightness) {
        Color shaded = applyShade(color, shadeIntensity);
        return applyBrightness(shaded, brightness);
    }

    /**
     * Create a color with transparency.
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * Draw a grid on a Graphics2D context.
     */
    public static void drawGrid(Graphics2D g2d, int width, int height, double scale, double gridSpacing) {
        g2d.setColor(new Color(200, 200, 200, 80));
        g2d.setStroke(new BasicStroke(0.5f));

        double step = gridSpacing * scale;
        if (step < 5) return; // Don't draw if too dense

        for (double x = 0; x < width; x += step) {
            g2d.drawLine((int) x, 0, (int) x, height);
        }
        for (double y = 0; y < height; y += step) {
            g2d.drawLine(0, (int) y, width, (int) y);
        }
    }

    /**
     * Draw a selection highlight around a rectangular area.
     */
    public static void drawSelectionHighlight(Graphics2D g2d, int x, int y, int w, int h) {
        g2d.setColor(new Color(41, 128, 185, 180));
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10f, new float[]{6f, 4f}, 0f));
        g2d.drawRect(x - 3, y - 3, w + 6, h + 6);

        // Corner handles
        g2d.setColor(new Color(41, 128, 185));
        g2d.setStroke(new BasicStroke(1));
        int handleSize = 6;
        g2d.fillRect(x - handleSize / 2 - 3, y - handleSize / 2 - 3, handleSize, handleSize);
        g2d.fillRect(x + w - handleSize / 2 + 3, y - handleSize / 2 - 3, handleSize, handleSize);
        g2d.fillRect(x - handleSize / 2 - 3, y + h - handleSize / 2 + 3, handleSize, handleSize);
        g2d.fillRect(x + w - handleSize / 2 + 3, y + h - handleSize / 2 + 3, handleSize, handleSize);
    }

    /**
     * Create an isometric polygon (top face of a box).
     */
    public static Path2D.Double createIsoTopFace(double x, double y, double z, double w, double d) {
        Point2D.Double p1 = toIsometric(x, y, z);
        Point2D.Double p2 = toIsometric(x + w, y, z);
        Point2D.Double p3 = toIsometric(x + w, y + d, z);
        Point2D.Double p4 = toIsometric(x, y + d, z);

        Path2D.Double path = new Path2D.Double();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.lineTo(p4.x, p4.y);
        path.closePath();
        return path;
    }

    /**
     * Create the right face of an isometric box.
     */
    public static Path2D.Double createIsoRightFace(double x, double y, double z, double w, double d, double h) {
        Point2D.Double p1 = toIsometric(x + w, y, z);
        Point2D.Double p2 = toIsometric(x + w, y + d, z);
        Point2D.Double p3 = toIsometric(x + w, y + d, z - h);
        Point2D.Double p4 = toIsometric(x + w, y, z - h);

        Path2D.Double path = new Path2D.Double();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.lineTo(p4.x, p4.y);
        path.closePath();
        return path;
    }

    /**
     * Create the left (front) face of an isometric box.
     */
    public static Path2D.Double createIsoLeftFace(double x, double y, double z, double w, double d, double h) {
        Point2D.Double p1 = toIsometric(x, y + d, z);
        Point2D.Double p2 = toIsometric(x + w, y + d, z);
        Point2D.Double p3 = toIsometric(x + w, y + d, z - h);
        Point2D.Double p4 = toIsometric(x, y + d, z - h);

        Path2D.Double path = new Path2D.Double();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.lineTo(p4.x, p4.y);
        path.closePath();
        return path;
    }

    /**
     * Draw an isometric box with proper face shading.
     */
    public static void drawIsoBox(Graphics2D g2d, double x, double y, double z,
                                   double w, double d, double h, Color color, boolean selected) {
        // Top face (brightest)
        Path2D.Double top = createIsoTopFace(x, y, z, w, d);
        g2d.setColor(lighten(color, 0.2));
        g2d.fill(top);
        g2d.setColor(darken(color, 0.7));
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(top);

        // Right face (medium)
        Path2D.Double right = createIsoRightFace(x, y, z, w, d, h);
        g2d.setColor(darken(color, 0.8));
        g2d.fill(right);
        g2d.setColor(darken(color, 0.6));
        g2d.draw(right);

        // Left face (darkest)
        Path2D.Double left = createIsoLeftFace(x, y, z, w, d, h);
        g2d.setColor(darken(color, 0.65));
        g2d.fill(left);
        g2d.setColor(darken(color, 0.5));
        g2d.draw(left);

        // Selection glow
        if (selected) {
            g2d.setColor(new Color(41, 128, 185, 100));
            g2d.setStroke(new BasicStroke(3));
            g2d.draw(top);
            g2d.draw(right);
            g2d.draw(left);
        }
    }

    /**
     * Draw a furniture label in 2D view.
     */
    public static void drawLabel(Graphics2D g2d, String text, int x, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        // Background
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRoundRect(x - textWidth / 2 - 4, y - textHeight / 2 - 2, textWidth + 8, textHeight + 4, 4, 4);

        // Text
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x - textWidth / 2, y + fm.getAscent() / 2);
    }

    /**
     * Get furniture shape icon/representation based on type for 2D view.
     */
    public static Shape getFurnitureShape2D(Furniture2DInfo info) {
        return switch (info.type) {
            case "CHAIR" -> createChairShape(info);
            case "SOFA" -> createSofaShape(info);
            default -> new java.awt.geom.Rectangle2D.Double(info.x, info.y, info.w, info.h);
        };
    }

    private static Shape createChairShape(Furniture2DInfo info) {
        Path2D.Double path = new Path2D.Double();
        // Main seat
        path.append(new java.awt.geom.Rectangle2D.Double(info.x, info.y, info.w, info.h), false);
        // Backrest
        path.append(new java.awt.geom.Rectangle2D.Double(info.x, info.y, info.w, info.h * 0.2), false);
        return path;
    }

    private static Shape createSofaShape(Furniture2DInfo info) {
        Path2D.Double path = new Path2D.Double();
        double armWidth = info.w * 0.1;
        // Main body
        path.append(new java.awt.geom.Rectangle2D.Double(info.x, info.y, info.w, info.h), false);
        // Left arm
        path.append(new java.awt.geom.Rectangle2D.Double(info.x, info.y, armWidth, info.h), false);
        // Right arm
        path.append(new java.awt.geom.Rectangle2D.Double(info.x + info.w - armWidth, info.y, armWidth, info.h), false);
        // Back
        path.append(new java.awt.geom.Rectangle2D.Double(info.x, info.y, info.w, info.h * 0.25), false);
        return path;
    }

    /**
     * Helper record for 2D furniture rendering info.
     */
    public static class Furniture2DInfo {
        public String type;
        public double x, y, w, h;

        public Furniture2DInfo(String type, double x, double y, double w, double h) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}
