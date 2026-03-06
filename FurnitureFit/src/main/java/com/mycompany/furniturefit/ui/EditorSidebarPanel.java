package com.mycompany.furniturefit.ui;

import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Floating left tool sidebar for the design editor (PNG icon version).
 */
public class EditorSidebarPanel extends JPanel {

    public interface ToolSelectionListener {
        void onToolSelected(int index);
    }

    private static final Color GREEN = new Color(45, 136, 45);
    private static final Color SIDEBAR_BG = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(210, 210, 210);
    private static final int ICON_SIZE = 26;
    private static final int BUTTON_SIZE = 48;

    private final JToggleButton[] sidebarButtons;

    public EditorSidebarPanel(ToolSelectionListener listener) {

        super(new MigLayout(
                "wrap 1, insets 14 10 14 10, gapy 12, aligny top",
                "[center]"
        ));

        setOpaque(false);
        setPreferredSize(new Dimension(68, 270));

        sidebarButtons = new JToggleButton[4];

        String[] tips = {"Room", "Furniture", "Light", "Cart"};
        String[] iconPaths = {
                "/icons/Home.png",
                "/icons/Armchair.png",
                "/icons/Light.png",
                "/icons/Shopping.png"
        };

        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < 4; i++) {

            final int idx = i;

            JToggleButton btn = new JToggleButton();
            btn.setIcon(loadIcon(iconPaths[i], ICON_SIZE));
            btn.setToolTipText(tips[i]);

            btn.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
            btn.setMinimumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
            btn.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
            btn.setHorizontalAlignment(SwingConstants.CENTER);
            btn.setVerticalAlignment(SwingConstants.CENTER);

            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setRolloverEnabled(true);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> listener.onToolSelected(idx));

            // Hover + Selected styling
            btn.addChangeListener(e -> {
                if (btn.isSelected()) {
                    btn.setOpaque(true);
                    btn.setContentAreaFilled(true);
                    btn.setBackground(new Color(231, 245, 231));
                } else if (btn.getModel().isRollover()) {
                    btn.setOpaque(true);
                    btn.setContentAreaFilled(true);
                    btn.setBackground(new Color(242, 242, 242));
                } else {
                    btn.setOpaque(false);
                    btn.setContentAreaFilled(false);
                }
            });

            sidebarButtons[i] = btn;
            group.add(btn);
            add(btn, "center");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(SIDEBAR_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

        g2.setColor(CARD_BORDER);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

        g2.dispose();
    }

    private ImageIcon loadIcon(String path, int size) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url == null) return null;
            BufferedImage img = ImageIO.read(url);
            if (img == null) return null;
            if (img.getWidth() == size && img.getHeight() == size) return new ImageIcon(img);

            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(img, 0, 0, size, size, null);
            g2.dispose();
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    public void clearSelection() {
        for (JToggleButton b : sidebarButtons) {
            b.setSelected(false);
        }
        repaint();
    }
}
