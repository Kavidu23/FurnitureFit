package com.mycompany.furniturefit.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycompany.furniturefit.model.Design;
import com.mycompany.furniturefit.model.Furniture;
import com.mycompany.furniturefit.model.Room;

import java.awt.Color;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Design operations.
 * Furniture data is serialized as JSON for storage.
 */
public class DesignDAO {

    private final DatabaseManager dbManager;
    private final Gson gson;

    public DesignDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Save a new design to the database.
     */
    public Design save(Design design) {
        String sql = """
            INSERT INTO designs (user_id, name, room_width, room_height, room_depth,
                room_shape, wall_color, floor_color, furniture_data)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            setInsertParams(pstmt, design);
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                design.setId(keys.getInt(1));
            }
            return design;
        } catch (SQLException e) {
            System.err.println("Save design error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Update an existing design.
     */
    public boolean update(Design design) {
        String sql = """
            UPDATE designs SET name = ?, room_width = ?, room_height = ?, room_depth = ?,
                room_shape = ?, wall_color = ?, floor_color = ?, furniture_data = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND user_id = ?
        """;

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            setUpdateParams(pstmt, design);
            pstmt.setInt(9, design.getId());
            pstmt.setInt(10, design.getUserId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update design error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Delete a design by ID.
     */
    public boolean delete(int designId, int userId) {
        String sql = "DELETE FROM designs WHERE id = ? AND user_id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, designId);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Delete design error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Find all designs for a user.
     */
    public List<Design> findByUserId(int userId) {
        String sql = "SELECT * FROM designs WHERE user_id = ? ORDER BY updated_at DESC";
        List<Design> designs = new ArrayList<>();

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                designs.add(mapDesign(rs));
            }
        } catch (SQLException e) {
            System.err.println("Find designs error: " + e.getMessage());
        }
        return designs;
    }

    /**
     * Find a single design by ID.
     */
    public Design findById(int designId) {
        String sql = "SELECT * FROM designs WHERE id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, designId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapDesign(rs);
            }
        } catch (SQLException e) {
            System.err.println("Find design error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Set PreparedStatement parameters from a Design object.
     */
    private void setInsertParams(PreparedStatement pstmt, Design design) throws SQLException {
        Room room = design.getRoom();
        pstmt.setInt(1, design.getUserId());
        pstmt.setString(2, design.getName());
        pstmt.setDouble(3, room.getWidth());
        pstmt.setDouble(4, room.getHeight());
        pstmt.setDouble(5, room.getDepth());
        pstmt.setString(6, room.getShape().name());
        pstmt.setString(7, colorToHex(room.getWallColor()));
        pstmt.setString(8, colorToHex(room.getFloorColor()));
        pstmt.setString(9, serializeFurniture(design.getFurnitureList()));
    }

    /**
     * Set PreparedStatement parameters for update SQL.
     */
    private void setUpdateParams(PreparedStatement pstmt, Design design) throws SQLException {
        Room room = design.getRoom();
        pstmt.setString(1, design.getName());
        pstmt.setDouble(2, room.getWidth());
        pstmt.setDouble(3, room.getHeight());
        pstmt.setDouble(4, room.getDepth());
        pstmt.setString(5, room.getShape().name());
        pstmt.setString(6, colorToHex(room.getWallColor()));
        pstmt.setString(7, colorToHex(room.getFloorColor()));
        pstmt.setString(8, serializeFurniture(design.getFurnitureList()));
        pstmt.setInt(9, design.getId());
        pstmt.setInt(10, design.getUserId());
    }

    /**
     * Map ResultSet row to Design object.
     */
    private Design mapDesign(ResultSet rs) throws SQLException {
        Design design = new Design();
        design.setId(rs.getInt("id"));
        design.setUserId(rs.getInt("user_id"));
        design.setName(rs.getString("name"));

        // Reconstruct room
        Room room = new Room();
        room.setWidth(rs.getDouble("room_width"));
        room.setHeight(rs.getDouble("room_height"));
        room.setDepth(rs.getDouble("room_depth"));
        room.setShape(Room.Shape.valueOf(rs.getString("room_shape")));
        room.setWallColor(hexToColor(rs.getString("wall_color")));
        room.setFloorColor(hexToColor(rs.getString("floor_color")));
        design.setRoom(room);

        // Reconstruct furniture list
        design.setFurnitureList(deserializeFurniture(rs.getString("furniture_data")));
        design.setCreatedAt(rs.getString("created_at"));
        design.setUpdatedAt(rs.getString("updated_at"));

        return design;
    }

    /**
     * Serialize furniture list to JSON, handling Color objects.
     */
    private String serializeFurniture(List<Furniture> furnitureList) {
        List<FurnitureDTO> dtos = new ArrayList<>();
        for (Furniture f : furnitureList) {
            FurnitureDTO dto = new FurnitureDTO();
            dto.id = f.getId();
            dto.type = f.getType().name();
            dto.name = f.getName();
            dto.x = f.getX();
            dto.y = f.getY();
            dto.width = f.getWidth();
            dto.depth = f.getDepth();
            dto.height = f.getHeight();
            dto.color = colorToHex(f.getColor());
            dto.rotation = f.getRotation();
            dto.shadeIntensity = f.getShadeIntensity();
            dtos.add(dto);
        }
        return gson.toJson(dtos);
    }

    /**
     * Deserialize JSON to furniture list.
     */
    private List<Furniture> deserializeFurniture(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }

        Type listType = new TypeToken<List<FurnitureDTO>>() {}.getType();
        List<FurnitureDTO> dtos = gson.fromJson(json, listType);
        List<Furniture> furnitureList = new ArrayList<>();

        for (FurnitureDTO dto : dtos) {
            Furniture f = new Furniture();
            f.setId(dto.id);
            f.setType(Furniture.Type.valueOf(dto.type));
            f.setName(dto.name);
            f.setX(dto.x);
            f.setY(dto.y);
            f.setWidth(dto.width);
            f.setDepth(dto.depth);
            f.setHeight(dto.height);
            f.setColor(hexToColor(dto.color));
            f.setRotation(dto.rotation);
            f.setShadeIntensity(dto.shadeIntensity);
            furnitureList.add(f);
        }
        return furnitureList;
    }

    private static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color hexToColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }

    /**
     * DTO for JSON serialization of Furniture (avoids Color serialization issues).
     */
    private static class FurnitureDTO {
        String id;
        String type;
        String name;
        double x, y, width, depth, height;
        String color;
        double rotation;
        double shadeIntensity;
    }
}
