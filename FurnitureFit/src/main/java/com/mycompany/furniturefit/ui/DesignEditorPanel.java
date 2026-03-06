package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.db.DesignDAO;
import com.mycompany.furnituredesignapp.graphics.Canvas2DPanel;
import com.mycompany.furnituredesignapp.graphics.Canvas3DPanel;
import com.mycompany.furnituredesignapp.graphics.OpenGLCanvas3D;
import com.mycompany.furnituredesignapp.model.Design;
import com.mycompany.furnituredesignapp.model.Furniture;
import com.mycompany.furnituredesignapp.model.Room;
import com.mycompany.furnituredesignapp.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main design editor panel matching the screenshot template.
 * Top toolbar: Save | Open | Help | Undo Redo | [2D/3D toggle] | Username+Avatar
 * Left floating sidebar: 4 icon buttons (Room, Furniture, Light, Cart)
 * Right floating properties panel (overlay)
 * Bottom-right floating zoom +/- buttons
 * 
 * This class has been refactored to use separate components:
 * - EditorToolbarPanel (top toolbar)
 * - EditorSidebarPanel (left sidebar)
 * - EditorPropertiesPanel (right properties panel)
 * - EditorUndoRedoManager (undo/redo)
 * - EditorFileHandler (file operations)
 */
public class DesignEditorPanel extends JPanel {

    // ── Colour palette ──
    private static final Color BG_CANVAS = new Color(75, 75, 75);

    // ── Core components ──
    private Design currentDesign;
    private User currentUser;
    private final Canvas2DPanel canvas2DPanel;
    private final Canvas3DPanel canvas3DPanel;
    private final OpenGLCanvas3D openGLCanvas3D;
    private final JPanel canvasContainer;
    private final CardLayout canvasCardLayout;
    private final DesignDAO designDAO;
    private boolean is3DView = false;

    // ── Extracted components ──
    private EditorToolbarPanel toolbarPanel;
    private EditorPropertiesPanel propertiesPanel;
    private EditorUndoRedoManager undoRedoManager;
    private EditorFileHandler fileHandler;

    // ── Other overlays ──
    private JPanel floatingHelpPanel;
    private JLayeredPane layeredCanvas;
    private JPanel sidebarPanel;
    private JPanel zoomPanel;

    private Runnable onBackToDashboard;
    private Runnable onOpenAccount;
    private Runnable onLogoutRequested;
    private boolean hasUnsavedChanges = false;
    private boolean nightMode = false;

    public DesignEditorPanel() {
        designDAO = new DesignDAO();
        setLayout(new BorderLayout());
        setBackground(BG_CANVAS);

        // ── Initialize canvas panels ──
        canvas2DPanel = new Canvas2DPanel();
        canvas3DPanel = new Canvas3DPanel();
        openGLCanvas3D = new OpenGLCanvas3D();

        // ── Initialize extracted components ──
        initToolbarPanel();
        initUndoRedoManager();
        initPropertiesPanel();
        initFileHandler();

        // ── Centre: layered pane holding canvas + overlays ──
        layeredCanvas = new JLayeredPane();
        layeredCanvas.setLayout(null);
        layeredCanvas.setBackground(BG_CANVAS);
        layeredCanvas.setOpaque(true);

        // Canvas card (2D / 3D)
        canvasCardLayout = new CardLayout();
        canvasContainer = new JPanel(canvasCardLayout);
        canvasContainer.add(canvas2DPanel, "2D");
        canvasContainer.add(openGLCanvas3D, "3D");
        canvasContainer.add(canvas3DPanel, "3D_FALLBACK");
        layeredCanvas.add(canvasContainer, JLayeredPane.DEFAULT_LAYER);

        // Floating sidebar
        sidebarPanel = new EditorSidebarPanel(this::handleSidebarClick);
        layeredCanvas.add(sidebarPanel, JLayeredPane.PALETTE_LAYER);

        // Floating properties panel
        layeredCanvas.add(propertiesPanel.getPanel(), JLayeredPane.PALETTE_LAYER);

        // Floating help (hidden initially)
        floatingHelpPanel = EditorHelpOverlayFactory.create(() -> floatingHelpPanel.setVisible(false));
        floatingHelpPanel.setVisible(false);
        layeredCanvas.add(floatingHelpPanel, JLayeredPane.PALETTE_LAYER);

        // Floating zoom controls
        zoomPanel = createFloatingZoom();
        layeredCanvas.add(zoomPanel, JLayeredPane.PALETTE_LAYER);

        // Keep everything positioned when resized
        layeredCanvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateOverlayBounds();
            }
        });

        add(layeredCanvas, BorderLayout.CENTER);

        installEditorShortcuts();
        setupCallbacks();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    updateOverlayBounds();
                    revalidate();
                    repaint();
                });
            }

            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    updateOverlayBounds();
                    revalidate();
                    repaint();
                });
            }
        });

        SwingUtilities.invokeLater(this::updateOverlayBounds);
    }

    private void updateOverlayBounds() {
        if (layeredCanvas == null || canvasContainer == null || sidebarPanel == null || zoomPanel == null) {
            return;
        }
        int pw = layeredCanvas.getWidth();
        int ph = layeredCanvas.getHeight();
        if (pw <= 0 || ph <= 0) {
            return;
        }
        canvasContainer.setBounds(0, 0, pw, ph);

        Dimension sidebarSize = sidebarPanel.getPreferredSize();
        sidebarPanel.setBounds(15, (ph - sidebarSize.height) / 2, sidebarSize.width, sidebarSize.height);

        Dimension propsSize = propertiesPanel.getPanel().getPreferredSize();
        int propsH = Math.min(propsSize.height, ph - 30);
        int propsX = pw - 270;
        propertiesPanel.getPanel().setBounds(propsX, 15, 255, propsH);

        // Help panel uses the exact same slot as properties panel.
        floatingHelpPanel.setBounds(propsX, 15, 255, propsH);

        zoomPanel.setBounds(pw - 60, ph - 100, 45, 80);
    }

    private void initToolbarPanel() {
        toolbarPanel = new EditorToolbarPanel(new EditorToolbarPanel.Callbacks() {
            @Override public void onBack() { 
                confirmAndGoDashboard();
            }
            @Override public void onSave() { 
                fileHandler.saveDesign(); 
            }
            @Override public void onOpen() { 
                fileHandler.openDesignFile(); 
            }
            @Override public void onHelp() { toggleHelp(); }
            @Override public void onUndo() { undoRedoManager.undo(); }
            @Override public void onRedo() { undoRedoManager.redo(); }
            @Override public void onSwitch2D() { switchToView2D(); }
            @Override public void onSwitch3D() { switchToView3D(); }
            @Override public void onToggleNight(boolean enabled) { toggleNightMode(); }
            @Override public void onProfile() { showProfilePopup(); }
        });
        add(toolbarPanel, BorderLayout.NORTH);
    }

    private void initUndoRedoManager() {
        undoRedoManager = new EditorUndoRedoManager(
            null, // will be set when design is loaded
            canvas2DPanel, 
            canvas3DPanel, 
            openGLCanvas3D
        );
        undoRedoManager.setListener(new EditorUndoRedoManager.UndoRedoListener() {
            @Override
            public void onUndoRedoStateChanged() {
                toolbarPanel.setUndoRedoEnabled(undoRedoManager.canUndo(), undoRedoManager.canRedo());
            }
        });
    }

    private void initPropertiesPanel() {
        propertiesPanel = new EditorPropertiesPanel(
            canvas2DPanel, 
            canvas3DPanel, 
            openGLCanvas3D,
            () -> undoRedoManager.pushUndo()
        );
    }

    private void initFileHandler() {
        fileHandler = new EditorFileHandler(
            null, // will be set when design is loaded
            canvas2DPanel,
            canvas3DPanel,
            openGLCanvas3D,
            this,
            (Frame) SwingUtilities.getWindowAncestor(this)
        );
        fileHandler.setListener(new EditorFileHandler.FileOperationListener() {
            @Override
            public void onDesignLoaded(Design design) {
                undoRedoManager.clear();
                toolbarPanel.setUndoRedoEnabled(false, false);
                propertiesPanel.hideProperties();
            }

            @Override
            public void onDesignSaved(String fileName) {
                hasUnsavedChanges = false;
            }

            @Override
            public void onError(String message) {
                // Error already shown in file handler
            }
        });
    }

    // ━━━━━━━━━━━━━━━━━━ KEYBOARD SHORTCUTS ━━━━━━━━━━━━━━━━━━
    private void installEditorShortcuts() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undoAction");
        getActionMap().put("undoAction", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { undoRedoManager.undo(); }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redoAction");
        getActionMap().put("redoAction", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { undoRedoManager.redo(); }
        });
    }

    private void handleSidebarClick(int idx) {
        switch (idx) {
            case 0 -> openRoomConfig();
            case 1 -> openFurniturePicker();
            case 2 -> openLightingDialog();
            case 3 -> openCart();
        }
    }

    // ━━━━━━━━━━━━━━━━━━ CALLBACKS ━━━━━━━━━━━━━━━━━━
    private void setupCallbacks() {
        canvas2DPanel.setOnBeforeModified(() -> undoRedoManager.pushUndo());

        canvas2DPanel.setOnSelectionChanged(() -> {
            Furniture selected = canvas2DPanel.getSelectedFurniture();
            canvas3DPanel.setSelectedFurniture(selected);
            openGLCanvas3D.setSelectedFurniture(selected);
            if (selected != null) {
                if (floatingHelpPanel.isVisible()) {
                    floatingHelpPanel.setVisible(false);
                    toolbarPanel.setHelpActive(false);
                }
                propertiesPanel.showPropertiesForFurniture(selected);
            } else if (!canvas2DPanel.isRoomSelected()) {
                propertiesPanel.hideProperties();
            }
        });

        canvas2DPanel.setOnRoomSelected(() -> {
            if (currentDesign != null && currentDesign.getRoom() != null) {
                if (floatingHelpPanel.isVisible()) {
                    floatingHelpPanel.setVisible(false);
                    toolbarPanel.setHelpActive(false);
                }
                propertiesPanel.showPropertiesForRoom(currentDesign.getRoom());
            }
        });

        canvas2DPanel.setOnDesignModified(() -> {
            hasUnsavedChanges = true;
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
        });
    }

    // ━━━━━━━━━━━━━━━━━━ PUBLIC API ━━━━━━━━━━━━━━━━━━

    public void loadDesign(Design design) {
        loadDesign(design, true);
    }

    public void loadDesign(Design design, boolean showRoomInitially) {
        this.currentDesign = design;
        
        // Update components with new design
        canvas2DPanel.setDesign(design);
        canvas2DPanel.setRoomVisible(showRoomInitially);
        canvas3DPanel.setDesign(design);
        canvas3DPanel.setRoomVisible(showRoomInitially);
        openGLCanvas3D.setDesign(design);
        openGLCanvas3D.setRoomVisible(showRoomInitially);
        
        // Update undo/redo manager with new design
        undoRedoManager = new EditorUndoRedoManager(design, canvas2DPanel, canvas3DPanel, openGLCanvas3D);
        undoRedoManager.setListener(new EditorUndoRedoManager.UndoRedoListener() {
            @Override
            public void onUndoRedoStateChanged() {
                toolbarPanel.setUndoRedoEnabled(undoRedoManager.canUndo(), undoRedoManager.canRedo());
            }
        });
        
        // Update file handler with new design
        fileHandler = new EditorFileHandler(design, canvas2DPanel, canvas3DPanel, openGLCanvas3D,
                this, (Frame) SwingUtilities.getWindowAncestor(this));
        fileHandler.setListener(new EditorFileHandler.FileOperationListener() {
            @Override
            public void onDesignLoaded(Design d) {
                undoRedoManager.clear();
                toolbarPanel.setUndoRedoEnabled(false, false);
                propertiesPanel.hideProperties();
            }
            @Override
            public void onDesignSaved(String name) { hasUnsavedChanges = false; }
            @Override
            public void onError(String msg) { }
        });
        
        propertiesPanel.hideProperties();
        hasUnsavedChanges = false;
        switchToView2D();
        toolbarPanel.setViewMode3D(false);
        SwingUtilities.invokeLater(() -> {
            updateOverlayBounds();
            revalidate();
            repaint();
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        toolbarPanel.setUser(user);
    }

    public void setOnBackToDashboard(Runnable callback) {
        this.onBackToDashboard = callback;
    }

    public void setOnOpenAccount(Runnable callback) {
        this.onOpenAccount = callback;
    }

    public void setOnLogoutRequested(Runnable callback) {
        this.onLogoutRequested = callback;
    }

    // ━━━━━━━━━━━━━━━━━━ ACTIONS ━━━━━━━━━━━━━━━━━━

    private void toggleHelp() {
        boolean nowVisible = !floatingHelpPanel.isVisible();
        if (nowVisible && propertiesPanel.getPanel().isVisible()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Close Properties panel first to open Help.",
                    "Help Unavailable",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        floatingHelpPanel.setVisible(nowVisible);
        if (nowVisible) {
            layeredCanvas.moveToFront(floatingHelpPanel);
            floatingHelpPanel.repaint();
        }
        toolbarPanel.setHelpActive(nowVisible);
    }

    private void toggleNightMode() {
        nightMode = toolbarPanel.isNightModeSelected();
        canvas3DPanel.setNightMode(nightMode);
        openGLCanvas3D.setNightMode(nightMode);
        if (nightMode && !is3DView) {
            switchToView3D();
            toolbarPanel.setViewMode3D(true);
        }
    }

    private void showProfilePopup() {
        Point screenPos = toolbarPanel.getProfileAnchorOnScreen();
        Dimension anchorSize = toolbarPanel.getProfileAnchorSize();
        String displayName = currentUser != null ? currentUser.getFullName() : "User";

        EditorProfilePopup.show(
                this,
                displayName,
                screenPos,
                anchorSize,
                this::confirmAndGoDashboard,
                this::confirmAndLogout
        );
    }

    private void confirmAndGoDashboard() {
        if (hasUnsavedChanges) {
            int saveChoice = JOptionPane.showConfirmDialog(
                    this,
                    "You have unsaved changes. Save before going to Dashboard?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (saveChoice == JOptionPane.CANCEL_OPTION || saveChoice == JOptionPane.CLOSED_OPTION) return;
            if (saveChoice == JOptionPane.YES_OPTION) {
                fileHandler.saveDesign();
                if (hasUnsavedChanges) return; // save cancelled or failed
            }
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to go to Dashboard?",
                "Confirm Navigation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION && onBackToDashboard != null) {
            onBackToDashboard.run();
        }
    }

    private void confirmAndLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?" + (hasUnsavedChanges ? "\nUnsaved changes will be lost." : ""),
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION && onLogoutRequested != null) {
            onLogoutRequested.run();
        }
    }

    public boolean confirmExitEditorWithUnsavedChanges() {
        if (!hasUnsavedChanges) return true;

        int saveChoice = JOptionPane.showConfirmDialog(
                this,
                "You have unsaved changes. Save before closing the application?\nIf not saved, your design may be lost.",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (saveChoice == JOptionPane.CANCEL_OPTION || saveChoice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (saveChoice == JOptionPane.YES_OPTION) {
            fileHandler.saveDesign();
            return !hasUnsavedChanges;
        }
        return true;
    }

    private void openRoomConfig() {
        if (currentDesign == null) return;
        RoomPickerDialog dialog = new RoomPickerDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            undoRedoManager.pushUndo();
            dialog.applyToRoom(currentDesign.getRoom());
            canvas2DPanel.setRoomVisible(true);
            canvas3DPanel.setRoomVisible(true);
            openGLCanvas3D.setRoomVisible(true);
            hasUnsavedChanges = true;
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.setDesign(currentDesign);
            openGLCanvas3D.repaint();
            if (floatingHelpPanel.isVisible()) {
                floatingHelpPanel.setVisible(false);
                toolbarPanel.setHelpActive(false);
            }
            propertiesPanel.showPropertiesForRoom(currentDesign.getRoom());
        }
    }

    private void openFurniturePicker() {
        if (currentDesign == null) return;
        if (!ensureRoomAddedBeforeItems()) return;
        FurniturePickerDialog dialog = new FurniturePickerDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            undoRedoManager.pushUndo();
            Furniture.Type type = dialog.getSelectedType();
            Furniture furniture = new Furniture(type, 0, 0);
            currentDesign.addFurniture(furniture);
            canvas2DPanel.setSelectedFurniture(furniture);
            if (floatingHelpPanel.isVisible()) {
                floatingHelpPanel.setVisible(false);
                toolbarPanel.setHelpActive(false);
            }
            propertiesPanel.showPropertiesForFurniture(furniture);
            hasUnsavedChanges = true;
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
        }
    }

    private void openLightingDialog() {
        if (currentDesign == null) return;
        if (!ensureRoomAddedBeforeItems()) return;
        LightingDialog dialog = new LightingDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            if (dialog.getSelectedLightType() != null) {
                undoRedoManager.pushUndo();
                Furniture.Type lightType = dialog.getSelectedLightType();
                Furniture light = new Furniture(lightType, 0, 0);
                light.setLightOn(true);
                light.setLightColor(dialog.getSelectedLightColor());
                light.setBrightness(dialog.getIntensity());
                light.setRotation(dialog.getDirection());
                currentDesign.addFurniture(light);
                canvas2DPanel.setSelectedFurniture(light);
                canvas2DPanel.repaint();
                canvas3DPanel.repaint();
                openGLCanvas3D.repaint();
                if (floatingHelpPanel.isVisible()) {
                    floatingHelpPanel.setVisible(false);
                    toolbarPanel.setHelpActive(false);
                }
                propertiesPanel.showPropertiesForFurniture(light);
            }
            hasUnsavedChanges = true;
            canvas2DPanel.repaint();
            canvas3DPanel.repaint();
            openGLCanvas3D.repaint();
        }
    }

    private void openCart() {
        if (currentDesign == null || currentDesign.getFurnitureList().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add some furniture first.", "Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EditorCartDialog.show(this, currentDesign.getFurnitureList(), this::getPrice);
    }

    private boolean ensureRoomAddedBeforeItems() {
        if (!canvas2DPanel.isRoomVisible()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please add a room first before adding furniture or lights.",
                    "Room Required",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        return true;
    }

    private void switchToView3D() {
        is3DView = true;
        boolean roomVisible = canvas2DPanel.isRoomVisible();
        openGLCanvas3D.setRoomVisible(roomVisible);
        canvas3DPanel.setRoomVisible(roomVisible);
        openGLCanvas3D.setDesign(currentDesign);
        canvas3DPanel.setDesign(currentDesign);
        canvasCardLayout.show(canvasContainer, "3D");
        openGLCanvas3D.startAnimator();
        if (sidebarPanel != null) sidebarPanel.setVisible(false);
        toolbarPanel.setViewMode3D(true);
    }

    private void switchToView2D() {
        if (is3DView) openGLCanvas3D.stopAnimator();
        is3DView = false;
        canvasCardLayout.show(canvasContainer, "2D");
        if (sidebarPanel != null) sidebarPanel.setVisible(true);
        toolbarPanel.setViewMode3D(false);
    }

    private JPanel createFloatingZoom() {
        return new EditorZoomPanel(
                () -> {
                    if (is3DView) openGLCanvas3D.zoomIn();
                    else canvas2DPanel.setZoom(canvas2DPanel.getZoom() * 1.2);
                },
                () -> {
                    if (is3DView) openGLCanvas3D.zoomOut();
                    else canvas2DPanel.setZoom(canvas2DPanel.getZoom() * 0.8);
                }
        );
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
            case PENDANT_LIGHT -> 12000;
            case FLOOR_LAMP_LIGHT -> 15000;
            case CEILING_LIGHT -> 18000;
            case WALL_LIGHT -> 9000;
            case SPOTLIGHT -> 7000;
            case TABLE_LAMP_LIGHT -> 6000;
        };
    }
}
