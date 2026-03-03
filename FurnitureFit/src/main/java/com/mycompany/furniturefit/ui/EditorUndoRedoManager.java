package com.mycompany.furnituredesignapp.ui;

import com.mycompany.furnituredesignapp.graphics.Canvas2DPanel;
import com.mycompany.furnituredesignapp.graphics.Canvas3DPanel;
import com.mycompany.furnituredesignapp.graphics.OpenGLCanvas3D;
import com.mycompany.furnituredesignapp.model.Design;
import com.mycompany.furnituredesignapp.model.Furniture;
import com.mycompany.furnituredesignapp.model.Room;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Extracted undo/redo manager for the design editor.
 * Handles state capture, restoration, and undo/redo operations.
 */
public class EditorUndoRedoManager {

    public interface UndoRedoListener {
        void onUndoRedoStateChanged();
    }

    // ── State Classes ──
    public static class FurnitureState {
        String id;
        Furniture.Type type;
        String name;
        double x, y, w, d, h, rot, brightness;
        int rgb;
        boolean lightOn;
    }

    public static class UndoState {
        List<FurnitureState> furniture;
        double roomW, roomD, roomH;
        Room.Shape shape;
        int wallRgb, floorRgb;
    }

    private final Deque<UndoState> undoStack = new ArrayDeque<>();
    private final Deque<UndoState> redoStack = new ArrayDeque<>();
    private static final int MAX_STACK_SIZE = 50;

    private final Design design;
    private final Canvas2DPanel canvas2DPanel;
    private final Canvas3DPanel canvas3DPanel;
    private final OpenGLCanvas3D openGLCanvas3D;
    private UndoRedoListener listener;

    public EditorUndoRedoManager(Design design, Canvas2DPanel canvas2DPanel, 
                                  Canvas3DPanel canvas3DPanel, OpenGLCanvas3D openGLCanvas3D) {
        this.design = design;
        this.canvas2DPanel = canvas2DPanel;
        this.canvas3DPanel = canvas3DPanel;
        this.openGLCanvas3D = openGLCanvas3D;
    }

    public void setListener(UndoRedoListener listener) {
        this.listener = listener;
    }

    /**
     * Capture current state and push onto undo stack, clearing redo stack.
     */
    public void pushUndo() {
        if (design == null) return;
        if (undoStack.size() >= MAX_STACK_SIZE) undoStack.pollLast();
        undoStack.push(captureState());
        redoStack.clear();
        notifyListener();
    }

    private UndoState captureState() {
        UndoState s = new UndoState();
        s.furniture = new ArrayList<>();
        for (Furniture f : design.getFurnitureList()) {
            FurnitureState fs = new FurnitureState();
            fs.id = f.getId();
            fs.type = f.getType();
            fs.name = f.getName();
            fs.x = f.getX();
            fs.y = f.getY();
            fs.w = f.getWidth();
            fs.d = f.getDepth();
            fs.h = f.getHeight();
            fs.rot = f.getRotation();
            fs.brightness = f.getBrightness();
            fs.lightOn = f.isLightOn();
            fs.rgb = f.getColor() != null ? f.getColor().getRGB() : 0xFFAAAAAA;
            s.furniture.add(fs);
        }
        Room r = design.getRoom();
        s.roomW = r.getWidth();
        s.roomD = r.getDepth();
        s.roomH = r.getHeight();
        s.shape = r.getShape();
        s.wallRgb = r.getWallColor() != null ? r.getWallColor().getRGB() : 0xFF969696;
        s.floorRgb = r.getFloorColor() != null ? r.getFloorColor().getRGB() : 0xFFB48C64;
        return s;
    }

    private void restoreState(UndoState s) {
        List<Furniture> list = design.getFurnitureList();
        list.clear();
        for (FurnitureState fs : s.furniture) {
            Furniture f = new Furniture(fs.type, fs.x, fs.y);
            f.setId(fs.id);
            f.setName(fs.name);
            f.setWidth(fs.w);
            f.setDepth(fs.d);
            f.setHeight(fs.h);
            f.setRotation(fs.rot);
            f.setBrightness(fs.brightness);
            f.setLightOn(fs.lightOn);
            f.setColor(new Color(fs.rgb, true));
            list.add(f);
        }
        Room r = design.getRoom();
        r.setWidth(s.roomW);
        r.setDepth(s.roomD);
        r.setHeight(s.roomH);
        r.setShape(s.shape);
        r.setWallColor(new Color(s.wallRgb, true));
        r.setFloorColor(new Color(s.floorRgb, true));
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        if (redoStack.size() >= MAX_STACK_SIZE) redoStack.pollLast();
        redoStack.push(captureState());
        restoreState(undoStack.pop());
        afterUndoRedo();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        if (undoStack.size() >= MAX_STACK_SIZE) undoStack.pollLast();
        undoStack.push(captureState());
        restoreState(redoStack.pop());
        afterUndoRedo();
    }

    private void afterUndoRedo() {
        canvas2DPanel.setSelectedFurniture(null);
        canvas2DPanel.repaint();
        canvas3DPanel.setDesign(design);
        canvas3DPanel.repaint();
        openGLCanvas3D.setDesign(design);
        openGLCanvas3D.repaint();
        notifyListener();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onUndoRedoStateChanged();
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        notifyListener();
    }
}
