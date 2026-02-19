package com.mycompany.furnituredesignapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete furniture design containing a room and furniture items.
 */
public class Design {

    private int id;
    private int userId;
    private String name;
    private Room room;
    private List<Furniture> furnitureList;
    private String createdAt;
    private String updatedAt;

    public Design() {
        this.room = new Room();
        this.furnitureList = new ArrayList<>();
    }

    public Design(int userId, String name) {
        this.userId = userId;
        this.name = name;
        this.room = new Room();
        this.furnitureList = new ArrayList<>();
    }

    // Furniture management
    public void addFurniture(Furniture furniture) {
        furnitureList.add(furniture);
    }

    public void removeFurniture(Furniture furniture) {
        furnitureList.remove(furniture);
    }

    public void removeFurnitureById(String furnitureId) {
        furnitureList.removeIf(f -> f.getId().equals(furnitureId));
    }

    public Furniture findFurnitureAt(double x, double y) {
        // Search in reverse order (top items first)
        for (int i = furnitureList.size() - 1; i >= 0; i--) {
            if (furnitureList.get(i).contains(x, y)) {
                return furnitureList.get(i);
            }
        }
        return null;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public List<Furniture> getFurnitureList() {
        return furnitureList;
    }

    public void setFurnitureList(List<Furniture> furnitureList) {
        this.furnitureList = furnitureList;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Design{id=" + id + ", name='" + name + "', furniture=" + furnitureList.size() + " items}";
    }
}
