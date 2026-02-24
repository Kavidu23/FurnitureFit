package com.mycompany.furnituredesignapp.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Help panel with user guide and application instructions.
 */
public class HelpPanel extends JPanel {

    private Runnable onBack;

    public HelpPanel() {
        setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
        setBackground(new Color(245, 245, 245));

        JPanel card = new JPanel(new MigLayout("wrap 1, insets 30, gapy 5", "[600!, fill]"));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210), 1));

        // Title
        JLabel title = new JLabel("Help & User Guide");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(56, 124, 43));
        card.add(title, "center, gapbottom 15");

        // Help content
        String helpHtml = """
            <html>
            <body style="color: #404040; font-family: 'Segoe UI'; font-size: 12px; line-height: 1.6;">
            
            <h3 style="color: #387C2B;">Getting Started</h3>
            <p>FurnitureFit Design Studio allows designers to create room layouts with furniture
            and visualize them in both 2D and 3D views.</p>
            
            <h3 style="color: #387C2B;">Creating a Design</h3>
            <ol>
                <li>Click <b>"+ New Design"</b> from the Dashboard</li>
                <li>Configure the room dimensions, shape, and colours using the <b>Room</b> button</li>
                <li>Add furniture using the <b>Furniture</b> button in the toolbar</li>
                <li>Arrange furniture by dragging them in the 2D canvas</li>
                <li>Save your design using the <b>Save</b> button</li>
            </ol>
            
            <h3 style="color: #387C2B;">2D Canvas Controls</h3>
            <ul>
                <li><b>Left Click</b> — Select furniture</li>
                <li><b>Left Drag</b> — Move selected furniture</li>
                <li><b>Right Click</b> — Context menu (color, rotate, delete)</li>
                <li><b>Alt + Drag</b> — Pan the view</li>
                <li><b>Scroll Wheel</b> — Zoom in/out</li>
                <li><b>R key</b> — Rotate selected furniture by 15°</li>
                <li><b>Delete key</b> — Delete selected furniture</li>
                <li><b>Ctrl+0</b> — Reset zoom and pan</li>
            </ul>
            
            <h3 style="color: #387C2B;">3D View Controls</h3>
            <ul>
                <li><b>Left Drag</b> — Orbit the perspective view (azimuth &amp; elevation)</li>
                <li><b>Middle Drag</b> — Pan the view</li>
                <li><b>Scroll Wheel</b> — Zoom in/out</li>
            </ul>
            
            <h3 style="color: #387C2B;">Toolbar Features</h3>
            <ul>
                <li><b>Room</b> — Set room size, shape (Rectangular, Square, L-shaped), wall & floor colours</li>
                <li><b>Furniture</b> — Browse and add furniture (chairs, tables, sofas, shelves, etc.)</li>
                <li><b>Shade</b> — Apply shading to selected or all furniture</li>
                <li><b>Zoom +/−</b> — Scale the view</li>
                <li><b>Fit</b> — Auto-scale all furniture to fit within the room</li>
                <li><b>Color</b> — Change color of selected or all furniture</li>
                <li><b>2D/3D</b> — Toggle between top-down 2D and perspective 3D views</li>
                <li><b>Save</b> — Save the current design to the database</li>
            </ul>
            
            <h3 style="color: #387C2B;">Properties Panel</h3>
            <p>When furniture is selected, the right-side Properties panel shows its position,
            size, rotation, shade, and color. All values can be edited directly.</p>
            
            <h3 style="color: #387C2B;">Managing Designs</h3>
            <ul>
                <li>Open saved designs from the Dashboard by clicking <b>Open</b> or double-clicking</li>
                <li>Delete designs using the <b>Delete</b> button on each design card</li>
                <li>Designs are stored locally in a SQLite database</li>
            </ul>
            
            </body>
            </html>
        """;

        JLabel helpContent = new JLabel(helpHtml);
        JScrollPane scrollPane = new JScrollPane(helpContent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scrollPane, "grow, push, h 400!");

        // Back button
        JButton backBtn = new JButton("← Back to Dashboard");
        backBtn.setBackground(new Color(56, 124, 43));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> { if (onBack != null) onBack.run(); });
        card.add(backBtn, "growx, h 36!, gaptop 15");

        add(card, "center");
    }

    public void setOnBack(Runnable callback) { this.onBack = callback; }
}
