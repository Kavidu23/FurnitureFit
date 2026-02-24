package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.db.DesignDAO;
import com.mycompany.furnituredesignapp.graphics.Canvas2DPanel;
import com.mycompany.furnituredesignapp.graphics.Canvas3DPanel;
import com.mycompany.furnituredesignapp.graphics.OpenGLCanvas3D;
import com.mycompany.furnituredesignapp.model.Design;
import com.mycompany.furnituredesignapp.model.Furniture;
import com.mycompany.furnituredesignapp.model.Room;
import com.mycompany.furnituredesignapp.model.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Main design editor panel matching the screenshot template.
 * Top toolbar: Save | Open | Help | Undo Redo | [2D/3D toggle] | Username+Avatar
 * Left floating sidebar: 4 icon buttons (Room, Furniture, Light, Cart)
 * Right floating properties panel (overlay)
 * Bottom-right floating zoom +/- buttons
 */
public class DesignEditorPanel extends JPanel {

    // ── Colour palette (matches screenshots — dark canvas, white toolbar/cards) ──
    private static final Color GREEN       = new Color(56, 124, 43);
    private static final Color BG_CANVAS   = new Color(75, 75, 75);   // dark grey grid bg
    private static final Color TOOLBAR_BG  = new Color(255, 255, 255);
    private static final Color TOOLBAR_FG  = new Color(50, 50, 50);
    private static final Color SIDEBAR_BG  = new Color(255, 255, 255);
    private static final Color CARD_BG     = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(210, 210, 210);

    // ── Core components ──
    private Design currentDesign;
    private User currentUser;
    private final Canvas2DPanel canvas2DPanel;
    private final Canvas3DPanel canvas3DPanel;   // Swing fallback (not shown unless JOGL unavailable)
    private final OpenGLCanvas3D openGLCanvas3D; // Primary OpenGL 3D view
    private final JPanel canvasContainer;
    private final CardLayout canvasCardLayout;
    private final DesignDAO designDAO;
    private boolean is3DView = false;

    // ── Overlays ──
    private JPanel floatingPropsPanel;
    private JPanel floatingHelpPanel;
    private JToggleButton view2DButton, view3DButton;
    private JLabel userNameLabel;
    private JPanel avatarPanel;
    private JToggleButton[] sidebarButtons;
    private JButton helpToolbarBtn;

    private Runnable onBackToDashboard;
    private boolean hasUnsavedChanges = false;

    // ── Properties sub-widgets ──
    private JLabel propTitleLabel;
    private JPanel propContentPanel;

    // ── Layered canvas host ──
    private JLayeredPane layeredCanvas;

    // ── Undo / Redo ──
    private final Deque<UndoState> undoStack = new ArrayDeque<>();
    private final Deque<UndoState> redoStack = new ArrayDeque<>();
    private JButton undoButton, redoButton;

    public DesignEditorPanel() {
        designDAO = new DesignDAO();
        setLayout(new BorderLayout());
        setBackground(BG_CANVAS);

        // ── Top Toolbar ──
        add(createToolbar(), BorderLayout.NORTH);

        // ── Centre: layered pane holding canvas + overlays ──
        layeredCanvas = new JLayeredPane();
        layeredCanvas.setLayout(null);
        layeredCanvas.setBackground(BG_CANVAS);
        layeredCanvas.setOpaque(true);

        // Canvas card (2D / 3D)
        canvasCardLayout = new CardLayout();
        canvasContainer = new JPanel(canvasCardLayout);
        canvas2DPanel = new Canvas2DPanel();
        canvas3DPanel = new Canvas3DPanel(); // Swing-based fallback (hidden)
        openGLCanvas3D = new OpenGLCanvas3D(); // OpenGL primary 3D
        canvasContainer.add(canvas2DPanel, "2D");
        canvasContainer.add(openGLCanvas3D, "3D");
        canvasContainer.add(canvas3DPanel, "3D_FALLBACK");
        layeredCanvas.add(canvasContainer, JLayeredPane.DEFAULT_LAYER);

        // Floating sidebar
        JPanel sidebar = createFloatingSidebar();
        layeredCanvas.add(sidebar, JLayeredPane.PALETTE_LAYER);

        // Floating properties (hidden initially)
        floatingPropsPanel = createFloatingProperties();
        floatingPropsPanel.setVisible(false);
        layeredCanvas.add(floatingPropsPanel, JLayeredPane.PALETTE_LAYER);

        // Floating help (hidden initially)
        floatingHelpPanel = createFloatingHelp();
        floatingHelpPanel.setVisible(false);
        layeredCanvas.add(floatingHelpPanel, JLayeredPane.PALETTE_LAYER);

        // Floating zoom controls
        JPanel zoomPanel = createFloatingZoom();
        layeredCanvas.add(zoomPanel, JLayeredPane.PALETTE_LAYER);

        // Keep everything positioned when resized
        layeredCanvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int pw = layeredCanvas.getWidth();
                int ph = layeredCanvas.getHeight();
                canvasContainer.setBounds(0, 0, pw, ph);
                sidebar.setBounds(15, (ph - sidebar.getPreferredSize().height) / 2,
                        sidebar.getPreferredSize().width, sidebar.getPreferredSize().height);
                int propsH = Math.min(floatingPropsPanel.getPreferredSize().height, ph - 30);
                floatingPropsPanel.setBounds(pw - 270, 15, 255, propsH);
                floatingHelpPanel.setBounds(pw - 285, 15, 270, Math.min(350, ph - 30));
                zoomPanel.setBounds(pw - 60, ph - 100, 45, 80);
            }
        });

        add(layeredCanvas, BorderLayout.CENTER);

        // Wire callbacks
        setupCallbacks();
    }

    // ━━━━━━━━━━━━━━━━━━ TOOLBAR (matches screenshot) ━━━━━━━━━━━━━━━━━━

    private JPanel createToolbar() {
        JPanel tb = new JPanel(new MigLayout("insets 6 14 6 14", "[][][][][10][][]push[]push[][]"));
        tb.setBackground(TOOLBAR_BG);
        tb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        // Dashboard home button — custom drawn house icon (no emoji dependency)
        JButton dashBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hov = getModel().isRollover();
                g2.setColor(hov ? GREEN : new Color(80, 80, 80));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth()/2, cy = getHeight()/2;
                int[] rx = {cx-9, cx, cx+9};
                int[] ry = {cy-1, cy-10, cy-1};
                g2.drawPolygon(rx, ry, 3);
                g2.drawRect(cx-6, cy-1, 12, 9);
                g2.drawRect(cx-2, cy+2, 4, 6);
                g2.dispose();
            }
        };
        dashBtn.setRolloverEnabled(true);
        dashBtn.setToolTipText("Back to Dashboard");
        dashBtn.setFocusPainted(false); dashBtn.setBorderPainted(false); dashBtn.setContentAreaFilled(false);
        dashBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dashBtn.setPreferredSize(new Dimension(32, 32));
        dashBtn.addActionListener(e -> {
            if (onBackToDashboard != null) onBackToDashboard.run();
        });
        tb.add(dashBtn);

        tb.add(toolTextBtn("Save", e -> saveDesign()));
        tb.add(toolTextBtn("Open", e -> openDesignFile()));
        helpToolbarBtn = toolTextBtn("Help", e -> toggleHelp());
        tb.add(helpToolbarBtn);

        // ── Undo / Redo (drawn arrows, wired to undo stack) ──
        undoButton = makeArrowBtn(false, "Undo (Ctrl+Z)");
        redoButton = makeArrowBtn(true,  "Redo (Ctrl+Y)");
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        undoButton.addActionListener(e -> undo());
        redoButton.addActionListener(e -> redo());
        tb.add(undoButton);
        tb.add(redoButton);

        // ── 2D / 3D pill toggle (centred) ──
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
            switchToView2D();
            stylePillButton(view2DButton, true);
            stylePillButton(view3DButton, false);
        });
        view3DButton.addActionListener(e -> {
            switchToView3D();
            stylePillButton(view2DButton, false);
            stylePillButton(view3DButton, true);
        });

        togglePill.add(view2DButton, "w 48!, h 30!");
        togglePill.add(view3DButton, "w 48!, h 30!");
        tb.add(togglePill);

        // ── Username + avatar (right side) ──
        userNameLabel = new JLabel("Designer");
        userNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userNameLabel.setForeground(TOOLBAR_FG);

        avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Green ring
                g2.setColor(GREEN);
                g2.fillOval(0, 0, 34, 34);
                // Avatar inner circle (light lavender)
                g2.setColor(new Color(220, 215, 235));
                g2.fillOval(3, 3, 28, 28);
                // Initials
                g2.setColor(new Color(80, 70, 110));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String init = currentUser != null ? currentUser.getFullName().substring(0, 1).toUpperCase() : "U";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, 17 - fm.stringWidth(init) / 2, 22);
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(34, 34));
        avatarPanel.setOpaque(false);
        avatarPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Profile popup on click
        avatarPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showProfilePopup(); }
        });
        userNameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userNameLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showProfilePopup(); }
        });
        tb.add(userNameLabel);
        tb.add(avatarPanel);

        return tb;
    }

    private JButton toolTextBtn(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(TOOLBAR_FG);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(GREEN); }
            @Override public void mouseExited(MouseEvent e)  { b.setForeground(TOOLBAR_FG); }
        });
        b.addActionListener(al);
        return b;
    }

    private JButton toolIconBtn(String symbol, String tip) {
        JButton b = new JButton(symbol);
        b.setToolTipText(tip);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        b.setForeground(TOOLBAR_FG);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void stylePillButton(JToggleButton b, boolean active) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (active) {
            b.setBackground(GREEN);
            b.setForeground(Color.WHITE);
            b.setBorderPainted(false);
            b.setContentAreaFilled(true);
            b.setOpaque(true);
        } else {
            b.setBackground(new Color(230, 230, 230));
            b.setForeground(new Color(100, 100, 100));
            b.setBorderPainted(false);
            b.setContentAreaFilled(true);
            b.setOpaque(true);
        }
    }

    // ━━━━━━━━━━━━━━━━━━ FLOATING LEFT SIDEBAR ━━━━━━━━━━━━━━━━━━

    private JPanel createFloatingSidebar() {
        JPanel sb = new JPanel(new MigLayout("wrap 1, insets 8, gapy 6", "[center]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SIDEBAR_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(CARD_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        sb.setOpaque(false);
        sb.setPreferredSize(new Dimension(52, 240));

        sidebarButtons = new JToggleButton[4];
        String[] tips = {"Room", "Furniture", "Light", "Cart"};

        ButtonGroup sbGroup = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            JToggleButton btn = new JToggleButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // Draw hover/selected background
                    boolean sel = isSelected();
                    boolean hov = getModel().isRollover();
                    if (sel) {
                        g2.setColor(new Color(56, 124, 43, 30));
                        g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10);
                    } else if (hov) {
                        g2.setColor(new Color(0, 0, 0, 18));
                        g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10);
                    }
                    g2.setColor(sel ? GREEN : new Color(80, 80, 80));
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    drawSidebarIcon(g2, idx, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            btn.setRolloverEnabled(true);
            btn.setPreferredSize(new Dimension(40, 40));
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setToolTipText(tips[i]);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> handleSidebarClick(idx));
            sbGroup.add(btn);
            sidebarButtons[i] = btn;
            sb.add(btn, "w 40!, h 40!");
        }
        return sb;
    }

    private void drawSidebarIcon(Graphics2D g, int iconId, int w, int h) {
        int cx = w / 2, cy = h / 2;
        switch (iconId) {
            case 0 -> { // House / Room
                int[] xp = {cx, cx - 12, cx - 10, cx - 10, cx - 4, cx - 4, cx + 4, cx + 4, cx + 10, cx + 10, cx + 12};
                int[] yp = {cy - 12, cy - 1, cy - 1, cy + 10, cy + 10, cy + 3, cy + 3, cy + 10, cy + 10, cy - 1, cy - 1};
                g.drawPolygon(xp, yp, xp.length);
            }
            case 1 -> { // Furniture (bed icon)
                g.drawRect(cx - 11, cy - 5, 22, 14);
                g.drawLine(cx - 11, cy + 2, cx + 11, cy + 2);
                g.drawRect(cx - 9, cy - 3, 6, 5);
            }
            case 2 -> { // Lightbulb
                g.drawOval(cx - 7, cy - 10, 14, 14);
                g.drawLine(cx - 4, cy + 4, cx - 4, cy + 8);
                g.drawLine(cx + 4, cy + 4, cx + 4, cy + 8);
                g.drawLine(cx - 5, cy + 8, cx + 5, cy + 8);
            }
            case 3 -> { // Shopping cart
                g.drawLine(cx - 12, cy - 8, cx - 8, cy - 8);
                g.drawLine(cx - 8, cy - 8, cx - 5, cy + 4);
                g.drawLine(cx - 5, cy + 4, cx + 8, cy + 4);
                g.drawLine(cx + 8, cy + 4, cx + 10, cy - 4);
                g.drawLine(cx + 10, cy - 4, cx - 6, cy - 4);
                g.fillOval(cx - 4, cy + 6, 5, 5);
                g.fillOval(cx + 4, cy + 6, 5, 5);
            }
        }
    }

    private void handleSidebarClick(int idx) {
        switch (idx) {
            case 0 -> openRoomConfig();
            case 1 -> openFurniturePicker();
            case 2 -> openLightingDialog();
            case 3 -> openCart();
        }
    }

    // ━━━━━━━━━━━━━━━━━━ FLOATING PROPERTIES ━━━━━━━━━━━━━━━━━━

    private JPanel createFloatingProperties() {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 14", "[grow, fill]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]"));
        header.setOpaque(false);
        propTitleLabel = new JLabel("Properties");
        propTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        propTitleLabel.setForeground(TOOLBAR_FG);
        header.add(propTitleLabel);
        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setForeground(new Color(100, 100, 100));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> floatingPropsPanel.setVisible(false));
        header.add(closeBtn);
        card.add(header);

        propContentPanel = new JPanel(new MigLayout("wrap 2, insets 0, gapy 4", "[right, 70!][grow, fill]"));
        propContentPanel.setOpaque(false);
        card.add(propContentPanel);

        card.add(new JSeparator(), "growx, gaptop 8, gapbottom 4");

        JLabel pickLabel = new JLabel("Pick a color");
        pickLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pickLabel.setForeground(TOOLBAR_FG);
        card.add(pickLabel);

        card.add(createColorGrid());

        return card;
    }

    private JPanel createColorGrid() {
        Color[] palette = {
            Color.RED, Color.ORANGE, Color.YELLOW, new Color(0, 200, 0),
            new Color(0, 200, 200), Color.BLUE, new Color(128, 0, 255), Color.MAGENTA,
            Color.PINK, new Color(255, 200, 0), new Color(200, 255, 0), new Color(0, 255, 128),
            new Color(0, 128, 255), new Color(64, 0, 128), new Color(128, 0, 64), new Color(255, 0, 128),
            new Color(128, 0, 0), new Color(128, 64, 0), new Color(128, 128, 0), new Color(0, 128, 0),
            new Color(0, 128, 128), new Color(0, 0, 128), new Color(64, 0, 64), new Color(128, 0, 128),
            new Color(64, 0, 0), new Color(64, 32, 0), new Color(64, 64, 0), new Color(0, 64, 0),
            new Color(0, 64, 64), new Color(0, 0, 64), new Color(32, 0, 64), new Color(64, 0, 64)
        };
        JPanel grid = new JPanel(new GridLayout(4, 8, 2, 2));
        grid.setOpaque(false);
        for (Color c : palette) {
            JButton cb = new JButton();
            cb.setPreferredSize(new Dimension(22, 22));
            cb.setBackground(c);
            cb.setFocusPainted(false);
            cb.setBorderPainted(true);
            cb.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
            cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cb.addActionListener(e -> applyColorToSelection(c));
            grid.add(cb);
        }
        return grid;
    }

    private void applyColorToSelection(Color c) {
        Furniture sel = canvas2DPanel.getSelectedFurniture();
        if (sel != null) {
            pushUndo();
            sel.setColor(c);
            hasUnsavedChanges = true;
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
        } else if (currentDesign != null && currentDesign.getRoom() != null) {
            pushUndo();
            currentDesign.getRoom().setWallColor(c);
            hasUnsavedChanges = true;
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
        }
    }

    public void showPropertiesForFurniture(Furniture f) {
        propContentPanel.removeAll();
        if (f == null) {
            floatingPropsPanel.setVisible(false);
            return;
        }
        propTitleLabel.setText("Properties");
        long price = getPrice(f.getType());
        addPropRow("Price :", "Rs. " + String.format("%,d", price));
        addPropRow("Name :", f.getName());
        addPropRow("Height :", String.format("%.0f cm", f.getHeight() * 100));
        addPropRow("Width :", String.format("%.0f cm", f.getWidth() * 100));
        floatingPropsPanel.setVisible(true);
        floatingPropsPanel.revalidate();
        floatingPropsPanel.repaint();
    }

    public void showPropertiesForRoom() {
        if (currentDesign == null || currentDesign.getRoom() == null) return;
        Room room = currentDesign.getRoom();
        propContentPanel.removeAll();
        propTitleLabel.setText("Properties");
        addPropRow("Name :", room.getShape().getDisplayName() + " room");
        addPropRow("Height :", String.format("%.0f cm", room.getHeight() * 100));
        addPropRow("Width :", String.format("%.0f cm", room.getWidth() * 100));
        floatingPropsPanel.setVisible(true);
        floatingPropsPanel.revalidate();
        floatingPropsPanel.repaint();
    }

    private void addPropRow(String label, String value) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(80, 80, 80));
        propContentPanel.add(lbl);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        val.setForeground(TOOLBAR_FG);
        propContentPanel.add(val);
    }

    // ━━━━━━━━━━━━━━━━━━ FLOATING HELP ━━━━━━━━━━━━━━━━━━

    private JPanel createFloatingHelp() {
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 14", "[grow, fill]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JPanel header = new JPanel(new MigLayout("insets 0", "[grow][]"));
        header.setOpaque(false);
        JLabel title = new JLabel("Help?");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.add(title);
        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setForeground(new Color(100, 100, 100));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> floatingHelpPanel.setVisible(false));
        header.add(closeBtn);
        card.add(header);

        JTextArea helpArea = new JTextArea();
        helpArea.setEditable(false);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helpArea.setBackground(new Color(245, 245, 245));
        JScrollPane sp = new JScrollPane(helpArea);
        sp.setPreferredSize(new Dimension(0, 100));
        sp.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        card.add(sp, "h 100!");

        // Ask AI field
        JPanel askPanel = new JPanel(new MigLayout("insets 0", "[grow][]"));
        askPanel.setOpaque(false);
        JTextField askField = new JTextField();
        askField.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        askField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        askField.setText("Ask AI......");
        askField.setForeground(Color.GRAY);
        askField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (askField.getText().equals("Ask AI......")) { askField.setText(""); askField.setForeground(Color.BLACK); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (askField.getText().isEmpty()) { askField.setText("Ask AI......"); askField.setForeground(Color.GRAY); }
            }
        });
        askPanel.add(askField, "growx, h 28!");
        JButton sendBtn = new JButton("\u27A1");
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendBtn.setFocusPainted(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setContentAreaFilled(false);
        askPanel.add(sendBtn, "w 30!, h 28!");
        card.add(askPanel, "gaptop 6");

        JLabel faqLabel = new JLabel("Frequently asked questions");
        faqLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        faqLabel.setForeground(new Color(80, 80, 80));
        card.add(faqLabel, "gaptop 8");

        JButton q1 = linkButton("How to save a project?");
        q1.addActionListener(e -> helpArea.setText("Click 'Save' in the toolbar to save your design. If new, you'll be prompted to enter a name."));
        JButton q2 = linkButton("How to add furniture?");
        q2.addActionListener(e -> helpArea.setText("Click the furniture icon (2nd) in the left sidebar to open the furniture picker and add items."));
        JButton q3 = linkButton("How to switch 2D/3D view?");
        q3.addActionListener(e -> helpArea.setText("Use the 2D/3D toggle in the top toolbar to switch between top-down and perspective views."));
        card.add(q1);
        card.add(q2);
        card.add(q3);

        return card;
    }

    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setForeground(GREEN);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        return b;
    }

    // ━━━━━━━━━━━━━━━━━━ FLOATING ZOOM ━━━━━━━━━━━━━━━━━━

    private JPanel createFloatingZoom() {
        JPanel zp = new JPanel(new MigLayout("wrap 1, insets 4, gapy 2", "[center]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        zp.setOpaque(false);
        zp.setPreferredSize(new Dimension(45, 80));

        JButton plus = makeZoomBtn("+");
        plus.addActionListener(e -> {
            if (is3DView) { openGLCanvas3D.zoomIn(); } else { canvas2DPanel.setZoom(canvas2DPanel.getZoom() * 1.2); }
        });

        JButton minus = makeZoomBtn("-");
        minus.addActionListener(e -> {
            if (is3DView) { openGLCanvas3D.zoomOut(); } else { canvas2DPanel.setZoom(canvas2DPanel.getZoom() * 0.8); }
        });

        zp.add(plus, "w 32!, h 32!");
        zp.add(minus, "w 32!, h 32!");
        return zp;
    }

    private JButton makeZoomBtn(String label) {
        JButton b = new JButton(label);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        b.setForeground(new Color(100, 100, 100));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ━━━━━━━━━━━━━━━━━━ CALLBACKS ━━━━━━━━━━━━━━━━━━

    private void setupCallbacks() {
        canvas2DPanel.setOnBeforeModified(() -> pushUndo());

        canvas2DPanel.setOnSelectionChanged(() -> {
            Furniture selected = canvas2DPanel.getSelectedFurniture();
            canvas3DPanel.setSelectedFurniture(selected);
            openGLCanvas3D.setSelectedFurniture(selected);
            if (selected != null) {
                showPropertiesForFurniture(selected);
            } else if (!canvas2DPanel.isRoomSelected()) {
                floatingPropsPanel.setVisible(false);
            }
        });

        canvas2DPanel.setOnRoomSelected(() -> {
            showPropertiesForRoom();
        });

        canvas2DPanel.setOnDesignModified(() -> {
            hasUnsavedChanges = true;
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
        });
    }

    // ━━━━━━━━━━━━━━━━━━ PUBLIC API ━━━━━━━━━━━━━━━━━━

    public void loadDesign(Design design) {
        this.currentDesign = design;
        canvas2DPanel.setDesign(design);
        canvas3DPanel.setDesign(design);
        openGLCanvas3D.setDesign(design);
        floatingPropsPanel.setVisible(false);
        hasUnsavedChanges = false;
        switchToView2D();
        view2DButton.setSelected(true);
        stylePillButton(view2DButton, true);
        stylePillButton(view3DButton, false);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            avatarPanel.repaint();
        }
    }

    public void setOnBackToDashboard(Runnable callback) {
        this.onBackToDashboard = callback;
    }

    // ━━━━━━━━━━━━━━━━━━ ACTIONS ━━━━━━━━━━━━━━━━━━

    private void toggleHelp() {
        boolean nowVisible = !floatingHelpPanel.isVisible();
        floatingHelpPanel.setVisible(nowVisible);
        if (helpToolbarBtn != null) {
            helpToolbarBtn.setForeground(nowVisible ? GREEN : TOOLBAR_FG);
        }
    }

    /**
     * Shows the profile popup dialog matching the screenshot design.
     * – Circular avatar with green ring
     * – Full name
     * – "My Projects" menu item
     * – "Log Out" menu item
     */
    private void showProfilePopup() {
        JDialog popup = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), false);
        popup.setUndecorated(true);
        popup.setBackground(new Color(0, 0, 0, 0));

        JPanel card = new JPanel(new net.miginfocom.swing.MigLayout("wrap 1, insets 24 28 24 28, gapy 6", "[center, 220!]")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(220, 220, 220));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // Close button (×) top-right
        JButton closeBtn = new JButton("\u2715");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeBtn.setForeground(new Color(120, 120, 120));
        closeBtn.setFocusPainted(false); closeBtn.setBorderPainted(false); closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> popup.dispose());

        JPanel topRow = new JPanel(new net.miginfocom.swing.MigLayout("insets 0", "[]push[]"));
        topRow.setOpaque(false);
        topRow.add(new JLabel()); // spacer
        topRow.add(closeBtn);
        card.add(topRow, "growx, gapbottom 6");

        // Avatar
        JPanel avatarBig = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Green ring
                g2.setColor(GREEN);
                g2.setStroke(new BasicStroke(3.5f));
                g2.drawOval(2, 2, 76, 76);
                // Lavender fill
                g2.setColor(new Color(220, 215, 235));
                g2.fillOval(6, 6, 68, 68);
                // Body (shirt)
                g2.setColor(new Color(128, 90, 180));
                g2.fillArc(18, 44, 44, 40, 0, 180);
                // Head
                g2.setColor(new Color(240, 190, 140));
                g2.fillOval(26, 18, 28, 28);
                // Initials fallback lettering (only if no image)
                g2.dispose();
            }
        };
        avatarBig.setPreferredSize(new Dimension(80, 80));
        avatarBig.setOpaque(false);
        card.add(avatarBig, "center, gapbottom 6");

        // Full name
        String displayName = currentUser != null ? currentUser.getFullName() : "User";
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLabel.setForeground(new Color(40, 40, 40));
        card.add(nameLabel, "center, gapbottom 14");

        // Divider
        card.add(new JSeparator(), "growx, gapbottom 10");

        // "My Projects" row
        JButton projectsBtn = popupMenuRow("\uD83D\uDCC1", "My Projects");
        projectsBtn.addActionListener(e -> {
            popup.dispose();
            if (onBackToDashboard != null) onBackToDashboard.run();
        });
        card.add(projectsBtn, "growx, h 40!");

        // "Log Out" row
        JButton logoutBtn = popupMenuRow("\u23FB", "Log Out");
        logoutBtn.setForeground(new Color(50, 50, 50));
        logoutBtn.addActionListener(e -> {
            popup.dispose();
            if (onBackToDashboard != null) onBackToDashboard.run();
        });
        card.add(logoutBtn, "growx, h 40!");

        popup.setContentPane(card);
        popup.pack();

        // Position below the avatar
        Point screenPos = avatarPanel.getLocationOnScreen();
        popup.setLocation(screenPos.x - popup.getWidth() + avatarPanel.getWidth(),
                          screenPos.y + avatarPanel.getHeight() + 6);
        popup.setVisible(true);

        // Dismiss on click outside
        popup.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override public void windowGainedFocus(java.awt.event.WindowEvent e) {}
            @Override public void windowLostFocus(java.awt.event.WindowEvent e)  { popup.dispose(); }
        });
    }

    /** Builds a row button for the profile popup. */
    private JButton popupMenuRow(String iconText, String labelText) {
        JButton b = new JButton();
        b.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(iconText);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        icon.setForeground(new Color(50, 50, 50));

        JLabel text = new JLabel(labelText);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        text.setForeground(new Color(50, 50, 50));

        b.add(icon);
        b.add(text);

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setContentAreaFilled(true); b.setBackground(new Color(245, 245, 245)); }
            @Override public void mouseExited(MouseEvent e)  { b.setContentAreaFilled(false); }
        });
        return b;
    }

    private void openDesignFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open Design");
        fc.showOpenDialog(this);
    }

    private void openRoomConfig() {
        if (currentDesign == null) return;
        RoomConfigDialog dialog = new RoomConfigDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), currentDesign.getRoom());
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            pushUndo();
            dialog.applyToRoom(currentDesign.getRoom());
            hasUnsavedChanges = true;
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            // Refresh OpenGL 3D view with updated room dimensions + colors
            openGLCanvas3D.setDesign(currentDesign);
            openGLCanvas3D.repaint();
            showPropertiesForRoom();
        }
    }

    private void openFurniturePicker() {
        if (currentDesign == null) return;
        FurniturePickerDialog dialog = new FurniturePickerDialog(
                (Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            pushUndo();
            Furniture.Type type = dialog.getSelectedType();
            Furniture furniture = new Furniture(type, 0, 0);
            currentDesign.addFurniture(furniture);
            canvas2DPanel.setSelectedFurniture(furniture);
            showPropertiesForFurniture(furniture);
            hasUnsavedChanges = true;
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
        }
    }

    private void openLightingDialog() {
        if (currentDesign == null) return;
        LightingDialog dialog = new LightingDialog(
                (Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            if (dialog.isApplyToAll()) {
                for (Furniture f : currentDesign.getFurnitureList()) {
                    f.setShadeIntensity(dialog.getIntensity());
                    // apply light direction via rotation tint (approximate)
                }
            } else {
                Furniture selected = canvas2DPanel.getSelectedFurniture();
                if (selected != null) selected.setShadeIntensity(dialog.getIntensity());
            }
            hasUnsavedChanges = true;
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();   // refresh OpenGL 3D view
        }
    }

    private void openCart() {
        if (currentDesign == null || currentDesign.getFurnitureList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add some furniture first.", "Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog cartDlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        cartDlg.setUndecorated(true);
        cartDlg.setBackground(new Color(0, 0, 0, 0));

        // ── Floating card ──────────────────────────────────────────────────────
        JPanel card = new JPanel(new MigLayout("wrap 1, insets 20 24 20 24, gapy 0", "[grow, fill]")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(210, 210, 210));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // Header
        JPanel headerRow = new JPanel(new MigLayout("insets 0, gapy 0", "[grow][]"));
        headerRow.setOpaque(false);
        JLabel cartTitle = new JLabel("Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cartTitle.setForeground(new Color(30, 30, 30));
        headerRow.add(cartTitle, "aligny center");
        JButton closeCart = new JButton("✕");
        closeCart.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeCart.setForeground(new Color(100, 100, 100));
        closeCart.setFocusPainted(false); closeCart.setBorderPainted(false); closeCart.setContentAreaFilled(false);
        closeCart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeCart.addActionListener(e -> cartDlg.dispose());
        headerRow.add(closeCart);
        card.add(headerRow, "growx, gapbottom 14");

        // Column headers
        JPanel colHdr = new JPanel(new MigLayout("insets 0", "[grow][][80!]"));
        colHdr.setOpaque(false);
        Font hdrFont = new Font("Segoe UI", Font.BOLD, 11);
        Color hdrClr = new Color(130, 130, 130);
        JLabel h1 = new JLabel("Item"); h1.setFont(hdrFont); h1.setForeground(hdrClr);
        JLabel h2 = new JLabel("Qty"); h2.setFont(hdrFont); h2.setForeground(hdrClr);
        JLabel h3 = new JLabel("Price"); h3.setFont(hdrFont); h3.setForeground(hdrClr);
        colHdr.add(h1); colHdr.add(h2, "gapleft 8"); colHdr.add(h3, "al right");
        card.add(colHdr, "growx, gapbottom 6");
        card.add(new JSeparator(), "growx, gapbottom 8");

        // Items
        long total = 0;
        for (Furniture f : currentDesign.getFurnitureList()) {
            long price = getPrice(f.getType());
            total += price;
            JPanel row = new JPanel(new MigLayout("insets 4 0 4 0", "[grow][][80!]"));
            row.setOpaque(false);
            // small colour dot
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(f.getColor());
                    g2.fillOval(0, 2, 10, 10);
                    g2.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(10, 14)); dot.setOpaque(false);
            JLabel nameL = new JLabel(f.getName());
            nameL.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            nameRow.setOpaque(false); nameRow.add(dot); nameRow.add(nameL);
            JLabel qtyL = new JLabel("x1");
            qtyL.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            qtyL.setForeground(new Color(120, 120, 120));
            JLabel priceL = new JLabel("Rs. " + String.format("%,d", price));
            priceL.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            row.add(nameRow, "grow");
            row.add(qtyL, "gapleft 8");
            row.add(priceL, "al right");
            card.add(row, "growx");
        }

        card.add(new JSeparator(), "growx, gapy 10");

        // Total row
        JPanel totalRow = new JPanel(new MigLayout("insets 4 0 0 0", "[grow][]"));
        totalRow.setOpaque(false);
        JLabel totLabel = new JLabel("Total");
        totLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel totValue = new JLabel("Rs. " + String.format("%,d", total));
        totValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totValue.setForeground(GREEN);
        totalRow.add(totLabel, "grow");
        totalRow.add(totValue);
        card.add(totalRow, "growx, gapbottom 14");

        // Continue button
        JButton contBtn = new JButton("Continue");
        contBtn.setBackground(GREEN); contBtn.setForeground(Color.WHITE);
        contBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contBtn.setFocusPainted(false); contBtn.setBorderPainted(false); contBtn.setOpaque(true);
        contBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        contBtn.addActionListener(e -> cartDlg.dispose());
        card.add(contBtn, "growx, h 40!");

        cartDlg.setContentPane(card);
        cartDlg.pack();
        cartDlg.setMinimumSize(new Dimension(380, cartDlg.getHeight()));
        cartDlg.setLocationRelativeTo(this);
        cartDlg.setVisible(true);
    }

    // ───────────────────────────────────────
    //  Undo / Redo infrastructure
    // ───────────────────────────────────────

    /** Snapshot of one furniture item’s state. */
    private static class FurnitureState {
        String id; Furniture.Type type; String name;
        double x, y, w, d, h, rot; int rgb;
    }

    /** Full design state snapshot for undo/redo. */
    private static class UndoState {
        List<FurnitureState> furniture;
        double roomW, roomD, roomH;
        Room.Shape shape;
        int wallRgb, floorRgb;
    }

    /** Capture current state and push onto undo stack, clearing redo stack. */
    private void pushUndo() {
        if (currentDesign == null) return;
        if (undoStack.size() >= 50) undoStack.pollLast();
        undoStack.push(captureState());
        redoStack.clear();
        updateUndoRedoButtons();
    }

    private UndoState captureState() {
        UndoState s = new UndoState();
        s.furniture = new ArrayList<>();
        for (Furniture f : currentDesign.getFurnitureList()) {
            FurnitureState fs = new FurnitureState();
            fs.id  = f.getId();   fs.type = f.getType();   fs.name = f.getName();
            fs.x   = f.getX();    fs.y    = f.getY();
            fs.w   = f.getWidth(); fs.d   = f.getDepth();  fs.h    = f.getHeight();
            fs.rot = f.getRotation();
            fs.rgb = f.getColor() != null ? f.getColor().getRGB() : 0xFFAAAAAA;
            s.furniture.add(fs);
        }
        Room r = currentDesign.getRoom();
        s.roomW = r.getWidth();  s.roomD = r.getDepth();  s.roomH = r.getHeight();
        s.shape = r.getShape();
        s.wallRgb  = r.getWallColor()  != null ? r.getWallColor().getRGB()  : 0xFF969696;
        s.floorRgb = r.getFloorColor() != null ? r.getFloorColor().getRGB() : 0xFFB48C64;
        return s;
    }

    private void restoreState(UndoState s) {
        List<Furniture> list = currentDesign.getFurnitureList();
        list.clear();
        for (FurnitureState fs : s.furniture) {
            Furniture f = new Furniture(fs.type, fs.x, fs.y);
            f.setId(fs.id);  f.setName(fs.name);
            f.setWidth(fs.w); f.setDepth(fs.d); f.setHeight(fs.h);
            f.setRotation(fs.rot);
            f.setColor(new Color(fs.rgb, true));
            list.add(f);
        }
        Room r = currentDesign.getRoom();
        r.setWidth(s.roomW);  r.setDepth(s.roomD);  r.setHeight(s.roomH);
        r.setShape(s.shape);
        r.setWallColor(new Color(s.wallRgb, true));
        r.setFloorColor(new Color(s.floorRgb, true));
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        if (redoStack.size() >= 50) redoStack.pollLast();
        redoStack.push(captureState());
        restoreState(undoStack.pop());
        afterUndoRedo();
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        if (undoStack.size() >= 50) undoStack.pollLast();
        undoStack.push(captureState());
        restoreState(redoStack.pop());
        afterUndoRedo();
    }

    private void afterUndoRedo() {
        canvas2DPanel.setSelectedFurniture(null);
        canvas2DPanel.repaint();
        canvas3DPanel.setDesign(currentDesign);
        canvas3DPanel.repaint();
        openGLCanvas3D.setDesign(currentDesign);
        openGLCanvas3D.repaint();
        floatingPropsPanel.setVisible(false);
        hasUnsavedChanges = true;
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        if (undoButton != null) undoButton.setEnabled(!undoStack.isEmpty());
        if (redoButton != null) redoButton.setEnabled(!redoStack.isEmpty());
    }

    /**
     * Creates a custom-drawn undo (↺) or redo (↻) button.
     * Uses pure Graphics2D so no font/emoji dependency.
     */
    private JButton makeArrowBtn(boolean clockwise, String tip) {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean en  = isEnabled();
                boolean hov = getModel().isRollover();
                g2.setColor(en ? (hov ? GREEN : new Color(80, 80, 80)) : new Color(190, 190, 190));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                // Arc
                int arcStart = clockwise ? 210 : 330;
                g2.drawArc(cx - 8, cy - 8, 16, 16, arcStart, 270);
                // Arrowhead
                double tip1 = Math.toRadians(clockwise ? arcStart + 270 : arcStart - 270);
                int ax = cx + (int)(8 * Math.cos(tip1)), ay = cy - (int)(8 * Math.sin(tip1));
                int[] hx = clockwise ?
                    new int[]{ax, ax - 5, ax + 1} : new int[]{ax, ax + 5, ax - 1};
                int[] hy = new int[]{ay, ay - 3, ay + 4};
                g2.fillPolygon(hx, hy, 3);
                g2.dispose();
            }
        };
        b.setRolloverEnabled(true);
        b.setToolTipText(tip);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setPreferredSize(new Dimension(30, 30));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private long getPrice(Furniture.Type type) {
        return switch (type) {
            case CHAIR -> 15000;
            case DINING_TABLE -> 45000;
            case SIDE_TABLE -> 10000;
            case SOFA -> 110000;
            case SHELF -> 25000;
            case COFFEE_TABLE -> 20000;
            case BED -> 120000;
            case WARDROBE -> 85000;
            case DESK -> 35000;
            case LAMP -> 8000;
        };
    }

    private void switchToView3D() {
        is3DView = true;
        openGLCanvas3D.setDesign(currentDesign);
        canvas3DPanel.setDesign(currentDesign);
        canvasCardLayout.show(canvasContainer, "3D");
        openGLCanvas3D.startAnimator();
    }

    private void switchToView2D() {
        if (is3DView) openGLCanvas3D.stopAnimator();
        is3DView = false;
        canvasCardLayout.show(canvasContainer, "2D");
    }

    private void saveDesign() {
        if (currentDesign == null) return;
        if (currentDesign.getId() == 0) {
            String name = JOptionPane.showInputDialog(this, "Enter design name:", "Save Design", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) return;
            currentDesign.setName(name.trim());
        }
        if (currentDesign.getId() == 0) {
            Design saved = designDAO.save(currentDesign);
            if (saved != null) {
                currentDesign.setId(saved.getId());
                JOptionPane.showMessageDialog(this, "Design saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            boolean updated = designDAO.update(currentDesign);
            if (updated) JOptionPane.showMessageDialog(this, "Design updated!", "Saved", JOptionPane.INFORMATION_MESSAGE);
        }
        hasUnsavedChanges = false;
    }
}
