package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.model.Furniture;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Properties panel showing and editing selected furniture attributes.
 */
public class PropertiesPanel extends JPanel {

    private Furniture selectedFurniture;

    private final JLabel nameLabel;
    private final JLabel typeLabel;
    private final JSpinner xSpinner, ySpinner;
    private final JSpinner widthSpinner, depthSpinner, heightSpinner;
    private final JSlider rotationSlider;
    private final JLabel rotationLabel;
    private final JSlider shadeSlider;
    private final JLabel shadeLabel;
    private final JSlider brightnessSlider;
    private final JLabel brightnessLabel;
    private final JButton colorButton;
    private final JPanel colorPreview;
    private final JButton deleteButton;
    private final JPanel detailsPanel;

    private boolean updating = false;
    private Runnable onPropertyChanged;
    private Runnable onDelete;

    public PropertiesPanel() {
        setLayout(new MigLayout("wrap 1, insets 10, gapy 0", "[grow, fill]"));
        setBackground(new Color(42, 47, 58));
        setPreferredSize(new Dimension(220, 0));

        // Header
        JLabel header = new JLabel("Properties");
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setForeground(new Color(200, 205, 215));
        add(header, "gapbottom 10");

        // Details panel (hidden when no selection)
        detailsPanel = new JPanel(new MigLayout("wrap 2, insets 0, gapy 4", "[right, 70!][grow, fill]"));
        detailsPanel.setOpaque(false);

        // Name & Type
        nameLabel = new JLabel("-");
        nameLabel.setForeground(new Color(65, 160, 230));
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        detailsPanel.add(createFieldLabel("Name:"));
        detailsPanel.add(nameLabel);

        typeLabel = new JLabel("-");
        typeLabel.setForeground(new Color(170, 175, 185));
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsPanel.add(createFieldLabel("Type:"));
        detailsPanel.add(typeLabel);

        // Separator
        detailsPanel.add(createSeparator(), "span 2, growx, gaptop 6, gapbottom 6");

        // Position
        detailsPanel.add(createSectionLabel("Position"), "span 2, left, gapbottom 2");
        xSpinner = createSpinner(-20, 20, 0.1);
        ySpinner = createSpinner(-20, 20, 0.1);
        detailsPanel.add(createFieldLabel("X (m):"));
        detailsPanel.add(xSpinner);
        detailsPanel.add(createFieldLabel("Y (m):"));
        detailsPanel.add(ySpinner);

        // Size
        detailsPanel.add(createSeparator(), "span 2, growx, gaptop 6, gapbottom 6");
        detailsPanel.add(createSectionLabel("Size"), "span 2, left, gapbottom 2");
        widthSpinner = createSpinner(0.1, 10, 0.1);
        depthSpinner = createSpinner(0.1, 10, 0.1);
        heightSpinner = createSpinner(0.1, 5, 0.1);
        detailsPanel.add(createFieldLabel("W (m):"));
        detailsPanel.add(widthSpinner);
        detailsPanel.add(createFieldLabel("D (m):"));
        detailsPanel.add(depthSpinner);
        detailsPanel.add(createFieldLabel("H (m):"));
        detailsPanel.add(heightSpinner);

        // Rotation
        detailsPanel.add(createSeparator(), "span 2, growx, gaptop 6, gapbottom 6");
        detailsPanel.add(createSectionLabel("Rotation"), "span 2, left, gapbottom 2");
        rotationSlider = new JSlider(0, 360, 0);
        rotationSlider.setOpaque(false);
        rotationSlider.setForeground(new Color(65, 160, 230));
        rotationLabel = new JLabel("0°");
        rotationLabel.setForeground(new Color(170, 175, 185));
        rotationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsPanel.add(rotationLabel, "right");
        detailsPanel.add(rotationSlider);

        // Shade
        detailsPanel.add(createSectionLabel("Shade"), "span 2, left, gaptop 4, gapbottom 2");
        shadeSlider = new JSlider(0, 100, 0);
        shadeSlider.setOpaque(false);
        shadeSlider.setForeground(new Color(65, 160, 230));
        shadeLabel = new JLabel("0%");
        shadeLabel.setForeground(new Color(170, 175, 185));
        shadeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsPanel.add(shadeLabel, "right");
        detailsPanel.add(shadeSlider);

        // Brightness
        detailsPanel.add(createSectionLabel("Brightness"), "span 2, left, gaptop 4, gapbottom 2");
        brightnessSlider = new JSlider(-100, 100, 0);
        brightnessSlider.setOpaque(false);
        brightnessSlider.setForeground(new Color(65, 160, 230));
        brightnessSlider.setMajorTickSpacing(50);
        brightnessSlider.setPaintTicks(true);
        brightnessLabel = new JLabel("0");
        brightnessLabel.setForeground(new Color(170, 175, 185));
        brightnessLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsPanel.add(brightnessLabel, "right");
        detailsPanel.add(brightnessSlider);

        // Color
        detailsPanel.add(createSeparator(), "span 2, growx, gaptop 6, gapbottom 6");
        detailsPanel.add(createFieldLabel("Color:"));
        JPanel colorPanel = new JPanel(new MigLayout("insets 0", "[24!][grow]"));
        colorPanel.setOpaque(false);
        colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(24, 24));
        colorPreview.setBorder(BorderFactory.createLineBorder(new Color(80, 85, 100)));
        colorButton = new JButton("Change");
        colorButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        colorButton.setFocusPainted(false);
        colorPanel.add(colorPreview);
        colorPanel.add(colorButton, "growx");
        detailsPanel.add(colorPanel);

        // Delete button
        detailsPanel.add(createSeparator(), "span 2, growx, gaptop 8, gapbottom 8");
        deleteButton = new JButton("Delete Furniture");
        deleteButton.setBackground(new Color(192, 57, 43));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteButton.setFocusPainted(false);
        deleteButton.setBorderPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detailsPanel.add(deleteButton, "span 2, growx, h 32!");

        add(detailsPanel);

        // No selection placeholder
        setupListeners();
        setSelectedFurniture(null);
    }

    private void setupListeners() {
        ChangeListener spinnerListener = e -> {
            if (!updating && selectedFurniture != null) {
                selectedFurniture.setX((double) xSpinner.getValue());
                selectedFurniture.setY((double) ySpinner.getValue());
                selectedFurniture.setWidth((double) widthSpinner.getValue());
                selectedFurniture.setDepth((double) depthSpinner.getValue());
                selectedFurniture.setHeight((double) heightSpinner.getValue());
                notifyChanged();
            }
        };
        xSpinner.addChangeListener(spinnerListener);
        ySpinner.addChangeListener(spinnerListener);
        widthSpinner.addChangeListener(spinnerListener);
        depthSpinner.addChangeListener(spinnerListener);
        heightSpinner.addChangeListener(spinnerListener);

        rotationSlider.addChangeListener(e -> {
            if (!updating && selectedFurniture != null) {
                selectedFurniture.setRotation(rotationSlider.getValue());
                rotationLabel.setText(rotationSlider.getValue() + "°");
                notifyChanged();
            }
        });

        shadeSlider.addChangeListener(e -> {
            if (!updating && selectedFurniture != null) {
                selectedFurniture.setShadeIntensity(shadeSlider.getValue() / 100.0);
                shadeLabel.setText(shadeSlider.getValue() + "%");
                notifyChanged();
            }
        });

        brightnessSlider.addChangeListener(e -> {
            if (!updating && selectedFurniture != null) {
                selectedFurniture.setBrightness(brightnessSlider.getValue() / 100.0);
                String label = brightnessSlider.getValue() > 0 ? "+" + brightnessSlider.getValue()
                        : String.valueOf(brightnessSlider.getValue());
                brightnessLabel.setText(label);
                notifyChanged();
            }
        });

        colorButton.addActionListener(e -> {
            if (selectedFurniture != null) {
                Color newColor = JColorChooser.showDialog(this, "Choose Furniture Color", selectedFurniture.getColor());
                if (newColor != null) {
                    selectedFurniture.setColor(newColor);
                    colorPreview.setBackground(newColor);
                    notifyChanged();
                }
            }
        });

        deleteButton.addActionListener(e -> {
            if (selectedFurniture != null && onDelete != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Delete " + selectedFurniture.getName() + "?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    onDelete.run();
                }
            }
        });
    }

    public void setSelectedFurniture(Furniture furniture) {
        this.selectedFurniture = furniture;
        updating = true;

        if (furniture == null) {
            detailsPanel.setVisible(false);
        } else {
            detailsPanel.setVisible(true);
            nameLabel.setText(furniture.getName());
            typeLabel.setText(furniture.getType().getDisplayName());
            xSpinner.setValue(furniture.getX());
            ySpinner.setValue(furniture.getY());
            widthSpinner.setValue(furniture.getWidth());
            depthSpinner.setValue(furniture.getDepth());
            heightSpinner.setValue(furniture.getHeight());
            rotationSlider.setValue((int) furniture.getRotation());
            rotationLabel.setText((int) furniture.getRotation() + "°");
            shadeSlider.setValue((int) (furniture.getShadeIntensity() * 100));
            shadeLabel.setText((int) (furniture.getShadeIntensity() * 100) + "%");
            brightnessSlider.setValue((int) (furniture.getBrightness() * 100));
            int bVal = (int) (furniture.getBrightness() * 100);
            brightnessLabel.setText(bVal > 0 ? "+" + bVal : String.valueOf(bVal));
            colorPreview.setBackground(furniture.getColor());
        }

        updating = false;
        revalidate();
        repaint();
    }

    private void notifyChanged() {
        if (onPropertyChanged != null) onPropertyChanged.run();
    }

    public void setOnPropertyChanged(Runnable callback) {
        this.onPropertyChanged = callback;
    }

    public void setOnDelete(Runnable callback) {
        this.onDelete = callback;
    }

    private JSpinner createSpinner(double min, double max, double step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(min, min, max, step));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return spinner;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(150, 155, 165));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return label;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(65, 160, 230));
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return label;
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 65, 78));
        return sep;
    }
}
