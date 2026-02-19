package com.mycompany.furnituredesignapp.model;

import java.awt.Color;

/**
 * Represents a piece of furniture that can be placed in a room.
 */
public class Furniture {

    public enum Type {
        CHAIR("Chair", 0.5, 0.5, 0.9),
        DINING_TABLE("Dining Table", 1.5, 0.9, 0.75),
        SIDE_TABLE("Side Table", 0.5, 0.5, 0.55),
        SOFA("Sofa", 2.0, 0.9, 0.85),
        SHELF("Shelf", 1.2, 0.3, 1.8),
        COFFEE_TABLE("Coffee Table", 1.0, 0.6, 0.45),
        BED("Bed", 2.0, 1.5, 0.6),
        WARDROBE("Wardrobe", 1.5, 0.6, 2.0),
        DESK("Desk", 1.2, 0.6, 0.75),
        LAMP("Floor Lamp", 0.3, 0.3, 1.5);

        private final String displayName;
        private final double defaultWidth;
        private final double defaultDepth;
        private final double defaultHeight;

        Type(String displayName, double defaultWidth, double defaultDepth, double defaultHeight) {
            this.displayName = displayName;
            this.defaultWidth = defaultWidth;
            this.defaultDepth = defaultDepth;
            this.defaultHeight = defaultHeight;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getDefaultWidth() {
            return defaultWidth;
        }

        public double getDefaultDepth() {
            return defaultDepth;
        }

        public double getDefaultHeight() {
            return defaultHeight;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private String id;
    private Type type;
    private String name;
    private double x;       // position X in room (meters)
    private double y;       // position Y in room (meters)
    private double width;   // meters
    private double depth;   // meters
    private double height;  // meters
    private Color color;
    private double rotation; // degrees 0-360
    private double shadeIntensity; // 0.0 to 1.0

    public Furniture() {
    }

    public Furniture(Type type, double x, double y) {
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.name = type.getDisplayName();
        this.x = x;
        this.y = y;
        this.width = type.getDefaultWidth();
        this.depth = type.getDefaultDepth();
        this.height = type.getDefaultHeight();
        this.color = getDefaultColor(type);
        this.rotation = 0;
        this.shadeIntensity = 0;
    }

    private Color getDefaultColor(Type type) {
        return switch (type) {
            case CHAIR -> new Color(139, 90, 43);       // brown wood
            case DINING_TABLE -> new Color(160, 110, 60); // oak
            case SIDE_TABLE -> new Color(180, 130, 80);   // light wood
            case SOFA -> new Color(70, 100, 140);         // blue fabric
            case SHELF -> new Color(120, 80, 40);         // dark wood
            case COFFEE_TABLE -> new Color(150, 105, 65); // walnut
            case BED -> new Color(200, 200, 210);         // light grey
            case WARDROBE -> new Color(140, 95, 50);      // medium wood
            case DESK -> new Color(170, 120, 70);         // birch
            case LAMP -> new Color(220, 200, 160);        // cream
        };
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation % 360;
    }

    public double getShadeIntensity() {
        return shadeIntensity;
    }

    public void setShadeIntensity(double shadeIntensity) {
        this.shadeIntensity = Math.max(0, Math.min(1, shadeIntensity));
    }

    /**
     * Check if a point (in room coordinates) is inside this furniture.
     */
    public boolean contains(double px, double py) {
        double dx = px - x;
        double dy = py - y;

        // Apply inverse rotation
        double rad = Math.toRadians(-rotation);
        double rx = dx * Math.cos(rad) - dy * Math.sin(rad);
        double ry = dx * Math.sin(rad) + dy * Math.cos(rad);

        return Math.abs(rx) <= width / 2 && Math.abs(ry) <= depth / 2;
    }

    @Override
    public String toString() {
        return name + " (" + type + ") at (" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ")";
    }
}
