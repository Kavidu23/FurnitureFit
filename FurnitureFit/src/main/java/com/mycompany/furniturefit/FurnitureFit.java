package com.mycompany.furnituredesignapp;

import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.furnituredesignapp.db.DatabaseManager;
import com.mycompany.furnituredesignapp.ui.MainFrame;

import javax.swing.*;

/**
 * FurnitureFit Design Studio — Main Application Entry Point.
 *
 * A desktop application for furniture designers to create room layouts,
 * add furniture, and visualize designs in 2D and 3D perspective views.
 *
 * Technologies: Java Swing, FlatLaf, MigLayout, SQLite, Java2D Graphics
 */
public class FurnitureFit  {

    public static void main(String[] args) {
        // Set FlatLaf Light theme (matches screenshot template)
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());

            // Custom UI defaults for better appearance
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("TabbedPane.showTabSeparators", true);
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to set FlatLaf theme: " + e.getMessage());
        }

        // Initialize database
        DatabaseManager.getInstance().initializeDatabase();

        // Launch application on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });

        // Shutdown hook to close database
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseManager.getInstance().close();
            System.out.println("Application shutdown. Database closed.");
        }));
    }
}
