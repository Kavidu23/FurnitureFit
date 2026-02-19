package com.mycompany.furnituredesignapp.model;

import java.awt.Color;

/**
 * Represents a room with dimensions, shape, and colour scheme.
 */
public class Room {

    public enum Shape {
        RECTANGULAR("Rectangular"),
        SQUARE("Square"),
        L_SHAPED("L-Shaped");

        private final String displayName;

        Shape(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private double width;   // in meters
    private double height;  // wall height in meters
    private double depth;   // depth/length in meters
    private Shape shape;
    private Color wallColor;
    private Color floorColor;

    public Room() {
        this.width = 5.0;
        this.height = 3.0;
        this.depth = 4.0;
        this.shape = Shape.RECTANGULAR;
        this.wallColor = new Color(245, 240, 230);  // warm white
        this.floorColor = new Color(180, 140, 100);  // wood tone
    }

    public Room(double width, double height, double depth, Shape shape, Color wallColor, Color floorColor) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.shape = shape;
        this.wallColor = wallColor;
        this.floorColor = floorColor;
    }

    // Getters and Setters
    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public Color getWallColor() {
        return wallColor;
    }

    public void setWallColor(Color wallColor) {
        this.wallColor = wallColor;
    }

    public Color getFloorColor() {
        return floorColor;
    }

    public void setFloorColor(Color floorColor) {
        this.floorColor = floorColor;
    }

    @Override
    public String toString() {
        return "Room{" + width + "x" + depth + "x" + height + "m, shape=" + shape + "}";
    }
}
