package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.model.Room;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for configuring room dimensions, shape, and colors.
 */
public class RoomConfigDialog extends JDialog {

    private final JSpinner widthSpinner;
    private final JSpinner depthSpinner;
    private final JSpinner heightSpinner;
    private final JComboBox<Room.Shape> shapeCombo;
    private final JPanel wallColorPreview;
    private final JPanel floorColorPreview;
    private Color wallColor;
    private Color floorColor;
    private boolean confirmed = false;

    public RoomConfigDialog(Frame owner, Room room) {
        super(owner, "Room Configuration", true);
        setSize(400, 450);
        setLocationRelativeTo(owner);
        setResizable(false);

        wallColor = room.getWallColor();
        floorColor = room.getFloorColor();

        JPanel content = new JPanel(new MigLayout("wrap 2, insets 20, gapy 10", "[right, 120!][grow, fill]"));

        // Title
        JLabel title = new JLabel("Configure Room");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        content.add(title, "span 2, center, gapbottom 15");

        // Shape
        content.add(new JLabel("Room Shape:"));
        shapeCombo = new JComboBox<>(Room.Shape.values());
        shapeCombo.setSelectedItem(room.getShape());
        content.add(shapeCombo);

        // Dimensions
        content.add(createSectionLabel("Dimensions"), "span 2, left, gaptop 10");

        content.add(new JLabel("Width (m):"));
        widthSpinner = new JSpinner(new SpinnerNumberModel(room.getWidth(), 1.0, 30.0, 0.5));
        content.add(widthSpinner);

        content.add(new JLabel("Depth (m):"));
        depthSpinner = new JSpinner(new SpinnerNumberModel(room.getDepth(), 1.0, 30.0, 0.5));
        content.add(depthSpinner);

        content.add(new JLabel("Height (m):"));
        heightSpinner = new JSpinner(new SpinnerNumberModel(room.getHeight(), 2.0, 6.0, 0.1));
        content.add(heightSpinner);

        // Colors
        content.add(createSectionLabel("Colors"), "span 2, left, gaptop 10");

        content.add(new JLabel("Wall Color:"));
        JPanel wallPanel = new JPanel(new MigLayout("insets 0", "[30!][grow]"));
        wallColorPreview = new JPanel();
        wallColorPreview.setBackground(wallColor);
        wallColorPreview.setPreferredSize(new Dimension(30, 25));
        wallColorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JButton wallColorBtn = new JButton("Choose...");
        wallColorBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Wall Color", wallColor);
            if (c != null) { wallColor = c; wallColorPreview.setBackground(c); }
        });
        wallPanel.add(wallColorPreview);
        wallPanel.add(wallColorBtn, "growx");
        content.add(wallPanel);

        content.add(new JLabel("Floor Color:"));
        JPanel floorPanel = new JPanel(new MigLayout("insets 0", "[30!][grow]"));
        floorColorPreview = new JPanel();
        floorColorPreview.setBackground(floorColor);
        floorColorPreview.setPreferredSize(new Dimension(30, 25));
        floorColorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JButton floorColorBtn = new JButton("Choose...");
        floorColorBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Floor Color", floorColor);
            if (c != null) { floorColor = c; floorColorPreview.setBackground(c); }
        });
        floorPanel.add(floorColorPreview);
        floorPanel.add(floorColorBtn, "growx");
        content.add(floorPanel);

        // Buttons
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[grow][grow]"));
        JButton okButton = new JButton("Apply");
        okButton.setBackground(new Color(41, 128, 185));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.addActionListener(e -> { confirmed = true; dispose(); });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton, "growx, h 35!");
        buttonPanel.add(cancelButton, "growx, h 35!");
        content.add(buttonPanel, "span 2, growx, gaptop 15");

        setContentPane(content);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void applyToRoom(Room room) {
        room.setShape((Room.Shape) shapeCombo.getSelectedItem());
        room.setWidth((double) widthSpinner.getValue());
        room.setDepth((double) depthSpinner.getValue());
        room.setHeight((double) heightSpinner.getValue());
        room.setWallColor(wallColor);
        room.setFloorColor(floorColor);
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(41, 128, 185));
        return label;
    }
}
