package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.model.User;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class EditorToolbarPanel extends JPanel {

    public interface Callbacks {
        void onBack();
        void onSave();
        void onOpen();
        void onHelp();
        void onUndo();
        void onRedo();
        void onSwitch2D();
        void onSwitch3D();
        void onToggleNight(boolean enabled);
        void onProfile();
    }

    private static final Color GREEN = new Color(45, 136, 45);
    private static final Color TOOLBAR_BG = Color.WHITE;
    private static final Color TOOLBAR_FG = new Color(50, 50, 50);
    private static final int TEXT_FONT_SIZE = 18; // Adjusted for better fit
    private static final int ARROW_ICON_SIZE = 22;
    private static final int ARROW_BUTTON_SIZE = 34;

    private final Callbacks callbacks;
    private User currentUser;

    private final JToggleButton view2DButton;
    private final JToggleButton view3DButton;
    private final JButton helpButton;
    private final JButton undoButton;
    private final JButton redoButton;
    private final JToggleButton nightModeButton;
    private final JLabel userNameLabel;
    private final JPanel avatarPanel;
    private final BufferedImage avatarImage;

    public EditorToolbarPanel(Callbacks callbacks) {
        this.callbacks = callbacks;
        this.avatarImage = loadAvatarImage();

        // [20] Left gap | [grow] push space | [] center | [grow] push space | [20] Right gap
        // This makes sure the Save button and Avatar are exactly the same distance from the screen edges.
        setLayout(new MigLayout("insets 0 20 0 20, fillx, aligny center", "[left][grow][center][grow][right]"));
        setBackground(TOOLBAR_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 225, 225)));
        setPreferredSize(new Dimension(0, 60));

        // --- LEFT SECTION ---
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(toolTextBtn("Home", e -> callbacks.onBack()));
        leftPanel.add(toolTextBtn("Save", e -> callbacks.onSave()));
        leftPanel.add(toolTextBtn("Open", e -> callbacks.onOpen()));
        helpButton = toolTextBtn("Help", e -> callbacks.onHelp());
        leftPanel.add(helpButton);
        
        undoButton = makeArrowBtn(false, "Undo (Ctrl+Z)");
        redoButton = makeArrowBtn(true, "Redo (Ctrl+Y)");
        undoButton.addActionListener(e -> callbacks.onUndo());
        redoButton.addActionListener(e -> callbacks.onRedo());
        leftPanel.add(undoButton);
        leftPanel.add(redoButton);
        
        add(leftPanel, "cell 0 0");

        // --- CENTER SECTION ---
        JPanel togglePill = new JPanel(new MigLayout("insets 2, gap 0", "[][]"));
        togglePill.setOpaque(false);

        view2DButton = new JToggleButton("2D");
        view3DButton = new JToggleButton("3D");
        ButtonGroup bg = new ButtonGroup();
        bg.add(view2DButton);
        bg.add(view3DButton);

        view2DButton.setSelected(true);
        stylePillButton(view2DButton, true);
        stylePillButton(view3DButton, false);

        view2DButton.addActionListener(e -> {
            stylePillButton(view2DButton, true);
            stylePillButton(view3DButton, false);
            callbacks.onSwitch2D();
        });
        view3DButton.addActionListener(e -> {
            stylePillButton(view2DButton, false);
            stylePillButton(view3DButton, true);
            callbacks.onSwitch3D();
        });

        // Width set to 65! to ensure text visibility
        togglePill.add(view2DButton, "w 65!, h 34!");
        togglePill.add(view3DButton, "w 65!, h 34!");
        add(togglePill, "cell 2 0, pad 0 -130 0 0");

        // --- RIGHT SECTION ---
        nightModeButton = new JToggleButton("Night");
        nightModeButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nightModeButton.setFocusPainted(false);
        nightModeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nightModeButton.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        nightModeButton.setContentAreaFilled(true);
        nightModeButton.setOpaque(true);
        nightModeButton.setVisible(false); // only show in 3D mode
        styleNightButton();
        nightModeButton.addActionListener(e -> {
            styleNightButton();
            callbacks.onToggleNight(nightModeButton.isSelected());
        });

        userNameLabel = new JLabel("Designer");
        userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        userNameLabel.setForeground(TOOLBAR_FG);

        avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GREEN);
                g2.fillOval(0, 0, 36, 36);
                g2.setColor(new Color(245, 245, 245));
                g2.fillOval(2, 2, 32, 32);
                if (avatarImage != null) {
                    g2.drawImage(avatarImage, 6, 6, 24, 24, null);
                }
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(36, 36));
        avatarPanel.setOpaque(false);
        avatarPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter profileClick = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { callbacks.onProfile(); }
        };
        userNameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userNameLabel.addMouseListener(profileClick);
        avatarPanel.addMouseListener(profileClick);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(nightModeButton);
        rightPanel.add(userNameLabel);
        rightPanel.add(avatarPanel);
        
        add(rightPanel, "cell 4 0");
    }

    private void stylePillButton(JToggleButton b, boolean active) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        if (active) {
            b.setBackground(GREEN);
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(new Color(235, 235, 235));
            b.setForeground(new Color(110, 110, 110));
        }
    }

    private JButton makeArrowBtn(boolean isRedo, String tip) {
        JButton b = new JButton();
        String path = isRedo ? "/icons/redo.png" : "/icons/undo.png";
        ImageIcon icon = getHighQualityIcon(path, ARROW_ICON_SIZE, ARROW_ICON_SIZE);
        
        if (icon != null) {
            b.setIcon(icon);
        } else {
            b.setText(isRedo ? "\u2192" : "\u2190");
            b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        }

        b.setToolTipText(tip);
        b.setPreferredSize(new Dimension(ARROW_BUTTON_SIZE, ARROW_BUTTON_SIZE));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setContentAreaFilled(true);
                    b.setBackground(new Color(245, 245, 245));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                b.setContentAreaFilled(false);
            }
        });
        return b;
    }

    private ImageIcon getHighQualityIcon(String path, int width, int height) {
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) return null;
            BufferedImage img = ImageIO.read(imgUrl);
            if (img == null) return null;
            if (img.getWidth() == width && img.getHeight() == height) return new ImageIcon(img);

            BufferedImage scaled = img;
            int currentW = img.getWidth();
            int currentH = img.getHeight();

            // Multi-step downscaling keeps thin icon strokes sharper.
            while (currentW / 2 >= width && currentH / 2 >= height) {
                currentW /= 2;
                currentH /= 2;
                BufferedImage tmp = new BufferedImage(currentW, currentH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = tmp.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.drawImage(scaled, 0, 0, currentW, currentH, null);
                g2.dispose();
                scaled = tmp;
            }

            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = resized.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(scaled, 0, 0, width, height, null);
            g2.dispose();
            return new ImageIcon(resized);
        } catch (Exception e) { return null; }
    }

    private JButton toolTextBtn(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 18));
        b.setForeground(TOOLBAR_FG);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { b.setForeground(GREEN); }
            @Override
            public void mouseExited(MouseEvent e) { b.setForeground(TOOLBAR_FG); }
        });
        b.addActionListener(al);
        return b;
    }

    private void styleNightButton() {
        if (nightModeButton.isSelected()) {
            nightModeButton.setBackground(new Color(40, 40, 45));
            nightModeButton.setForeground(Color.WHITE);
        } else {
            nightModeButton.setBackground(new Color(235, 235, 235));
            nightModeButton.setForeground(new Color(90, 90, 90));
        }
    }

    private BufferedImage loadAvatarImage() {
        try { return ImageIO.read(getClass().getResource("/images/avatar.png")); } 
        catch (Exception ex) { return null; }
    }

    // Existing helper methods for the Panel
    public void setViewMode3D(boolean is3D) {
        view3DButton.setSelected(is3D);
        view2DButton.setSelected(!is3D);
        stylePillButton(view2DButton, !is3D);
        stylePillButton(view3DButton, is3D);
        nightModeButton.setVisible(is3D);
    }
    public boolean isNightModeSelected() { return nightModeButton.isSelected(); }
    public Point getProfileAnchorOnScreen() {
        try { return avatarPanel.getLocationOnScreen(); } 
        catch (Exception ex) { return null; }
    }
    public Dimension getProfileAnchorSize() { return avatarPanel.getSize(); }
    public void setUser(User user) {
        this.currentUser = user;
        if (user != null && user.getFullName() != null) userNameLabel.setText(user.getFullName());
        avatarPanel.repaint();
    }
    public void setHelpActive(boolean active) { helpButton.setForeground(active ? GREEN : TOOLBAR_FG); }
    public void setUndoRedoEnabled(boolean u, boolean r) { undoButton.setEnabled(u); redoButton.setEnabled(r); }
}
