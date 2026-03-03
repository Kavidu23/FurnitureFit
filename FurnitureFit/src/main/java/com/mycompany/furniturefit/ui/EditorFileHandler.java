package com.mycompany.furnituredesignapp.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.furnituredesignapp.db.DesignDAO;
import com.mycompany.furnituredesignapp.graphics.Canvas2DPanel;
import com.mycompany.furnituredesignapp.graphics.Canvas3DPanel;
import com.mycompany.furnituredesignapp.graphics.OpenGLCanvas3D;
import com.mycompany.furnituredesignapp.model.Design;
import com.mycompany.furnituredesignapp.model.Furniture;
import com.mycompany.furnituredesignapp.model.Room;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Extracted file handler for the design editor.
 * Handles opening and saving design files (.fda format).
 */
public class EditorFileHandler {

    public interface FileOperationListener {
        void onDesignLoaded(Design design);
        void onDesignSaved(String fileName);
        void onError(String message);
    }

    private final Design design;
    private final Canvas2DPanel canvas2DPanel;
    private final Canvas3DPanel canvas3DPanel;
    private final OpenGLCanvas3D openGLCanvas3D;
    private final JPanel parentPanel;
    private final Frame parentFrame;
    private final DesignDAO designDAO;
    private FileOperationListener listener;

    public EditorFileHandler(Design design, Canvas2DPanel canvas2DPanel,
                             Canvas3DPanel canvas3DPanel, OpenGLCanvas3D openGLCanvas3D,
                             JPanel parentPanel, Frame parentFrame) {
        this.design = design;
        this.canvas2DPanel = canvas2DPanel;
        this.canvas3DPanel = canvas3DPanel;
        this.openGLCanvas3D = openGLCanvas3D;
        this.parentPanel = parentPanel;
        this.parentFrame = parentFrame;
        this.designDAO = new DesignDAO();
    }

    public void setListener(FileOperationListener listener) {
        this.listener = listener;
    }

    /**
     * Open a design file from disk.
     */
    public void openDesignFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open Design");
        fc.setFileFilter(new FileNameExtensionFilter("Furniture Design Files (*.fda)", "fda"));
        int result = fc.showOpenDialog(parentPanel);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            // Parse room
            JsonObject roomObj = root.getAsJsonObject("room");
            Room room = design.getRoom();
            if (roomObj != null) {
                if (roomObj.has("width")) room.setWidth(roomObj.get("width").getAsDouble());
                if (roomObj.has("depth")) room.setDepth(roomObj.get("depth").getAsDouble());
                if (roomObj.has("height")) room.setHeight(roomObj.get("height").getAsDouble());
                if (roomObj.has("shape")) room.setShape(Room.Shape.valueOf(roomObj.get("shape").getAsString()));
                if (roomObj.has("wallColor")) room.setWallColor(new Color(roomObj.get("wallColor").getAsInt(), true));
                if (roomObj.has("floorColor")) room.setFloorColor(new Color(roomObj.get("floorColor").getAsInt(), true));
            }

            // Parse furniture list
            design.getFurnitureList().clear();
            JsonArray furnitureArr = root.getAsJsonArray("furniture");
            if (furnitureArr != null) {
                for (JsonElement el : furnitureArr) {
                    JsonObject fo = el.getAsJsonObject();
                    Furniture.Type type = Furniture.Type.valueOf(fo.get("type").getAsString());
                    double fx = fo.get("x").getAsDouble();
                    double fy = fo.get("y").getAsDouble();
                    Furniture f = new Furniture(type, fx, fy);
                    if (fo.has("id")) f.setId(fo.get("id").getAsString());
                    if (fo.has("name")) f.setName(fo.get("name").getAsString());
                    if (fo.has("width")) f.setWidth(fo.get("width").getAsDouble());
                    if (fo.has("depth")) f.setDepth(fo.get("depth").getAsDouble());
                    if (fo.has("height")) f.setHeight(fo.get("height").getAsDouble());
                    if (fo.has("rotation")) f.setRotation(fo.get("rotation").getAsDouble());
                    if (fo.has("brightness")) f.setBrightness(fo.get("brightness").getAsDouble());
                    if (fo.has("shadeIntensity")) f.setShadeIntensity(fo.get("shadeIntensity").getAsDouble());
                    if (fo.has("lightOn")) f.setLightOn(fo.get("lightOn").getAsBoolean());
                    if (fo.has("color")) f.setColor(new Color(fo.get("color").getAsInt(), true));
                    if (fo.has("lightColor")) f.setLightColor(new Color(fo.get("lightColor").getAsInt(), true));
                    design.getFurnitureList().add(f);
                }
            }

            // Parse design name
            if (root.has("name")) {
                design.setName(root.get("name").getAsString());
            }

            // Refresh all views
            refreshViews();

            String message = "Design loaded: " + file.getName() + " (" + design.getFurnitureList().size() + " items)";
            JOptionPane.showMessageDialog(parentPanel, message, "Opened", JOptionPane.INFORMATION_MESSAGE);
            
            if (listener != null) {
                listener.onDesignLoaded(design);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentPanel, "Failed to open file: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            if (listener != null) {
                listener.onError("Failed to open: " + ex.getMessage());
            }
        }
    }

    /**
     * Save the current design to disk.
     */
    public void saveDesign() {
        if (design == null) return;

        // Ask user to pick or confirm a file name
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Design As");
        fc.setFileFilter(new FileNameExtensionFilter("Furniture Design Files (*.fda)", "fda"));
        if (design.getName() != null && !design.getName().isEmpty()) {
            fc.setSelectedFile(new File(design.getName() + ".fda"));
        } else {
            fc.setSelectedFile(new File("MyDesign.fda"));
        }
        int result = fc.showSaveDialog(parentPanel);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        // Ensure .fda extension
        if (!file.getName().toLowerCase().endsWith(".fda")) {
            file = new File(file.getAbsolutePath() + ".fda");
        }

        // Update design name from file name
        String nameFromFile = file.getName().replaceAll("\\.fda$", "");
        design.setName(nameFromFile);

        try {
            JsonObject root = new JsonObject();
            root.addProperty("name", design.getName());
            root.addProperty("version", "1.0");

            // Serialize room
            Room room = design.getRoom();
            JsonObject roomObj = new JsonObject();
            roomObj.addProperty("width", room.getWidth());
            roomObj.addProperty("depth", room.getDepth());
            roomObj.addProperty("height", room.getHeight());
            roomObj.addProperty("shape", room.getShape().name());
            roomObj.addProperty("wallColor", room.getWallColor() != null ? room.getWallColor().getRGB() : 0xFFF5F0E6);
            roomObj.addProperty("floorColor", room.getFloorColor() != null ? room.getFloorColor().getRGB() : 0xFFB48C64);
            root.add("room", roomObj);

            // Serialize furniture
            JsonArray furnitureArr = new JsonArray();
            for (Furniture f : design.getFurnitureList()) {
                JsonObject fo = new JsonObject();
                fo.addProperty("id", f.getId());
                fo.addProperty("type", f.getType().name());
                fo.addProperty("name", f.getName());
                fo.addProperty("x", f.getX());
                fo.addProperty("y", f.getY());
                fo.addProperty("width", f.getWidth());
                fo.addProperty("depth", f.getDepth());
                fo.addProperty("height", f.getHeight());
                fo.addProperty("rotation", f.getRotation());
                fo.addProperty("brightness", f.getBrightness());
                fo.addProperty("shadeIntensity", f.getShadeIntensity());
                fo.addProperty("lightOn", f.isLightOn());
                fo.addProperty("color", f.getColor() != null ? f.getColor().getRGB() : 0xFFAAAAAA);
                fo.addProperty("lightColor", f.getLightColor() != null ? f.getLightColor().getRGB() : 0xFFFFF0C8);
                furnitureArr.add(fo);
            }
            root.add("furniture", furnitureArr);

            // Write to file with pretty printing
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }

            // Also persist to dashboard database so it appears in Recent Designs.
            saveOrUpdateDashboardDesign();

            JOptionPane.showMessageDialog(parentPanel,
                    "Successfully saved.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);

            if (listener != null) {
                listener.onDesignSaved(file.getName());
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentPanel, "Failed to save: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            if (listener != null) {
                listener.onError("Failed to save: " + ex.getMessage());
            }
        }
    }

    /**
     * Refresh all canvas views with the current design.
     */
    public void refreshViews() {
        canvas2DPanel.setDesign(design);
        canvas3DPanel.setDesign(design);
        openGLCanvas3D.setDesign(design);
        canvas2DPanel.repaint();
        canvas3DPanel.repaint();
        openGLCanvas3D.repaint();
    }

    private void saveOrUpdateDashboardDesign() {
        if (design == null) return;
        if (design.getUserId() <= 0) return;

        if (design.getId() > 0) {
            boolean updated = designDAO.update(design);
            if (!updated) {
                Design saved = designDAO.save(design);
                if (saved != null) design.setId(saved.getId());
            }
        } else {
            Design saved = designDAO.save(design);
            if (saved != null) design.setId(saved.getId());
        }
    }
}
