package com.mycompany.furnituredesignapp.db;

import com.mycompany.furnituredesignapp.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

/**
 * Data Access Object for User operations.
 */
public class UserDAO {

    private final DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Register a new user. Returns the created User or null if username exists.
     */
    public User register(String username, String password, String fullName) {
        String hash = hashPassword(password);
        String sql = "INSERT INTO users (username, password_hash, full_name) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username.toLowerCase().trim());
            pstmt.setString(2, hash);
            pstmt.setString(3, fullName.trim());
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                User user = new User(username.toLowerCase().trim(), hash, fullName.trim());
                user.setId(keys.getInt(1));
                return user;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                return null; // Username already exists
            }
            System.err.println("Registration error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Authenticate a user. Returns User if credentials match, null otherwise.
     */
    public User login(String username, String password) {
        String hash = hashPassword(password);
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username.toLowerCase().trim());
            pstmt.setString(2, hash);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Find a user by username.
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username.toLowerCase().trim());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Find user error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Update user's password.
     */
    public boolean updatePassword(int userId, String oldPassword, String newPassword) {
        String oldHash = hashPassword(oldPassword);
        String sql = "UPDATE users SET password_hash = ? WHERE id = ? AND password_hash = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, hashPassword(newPassword));
            pstmt.setInt(2, userId);
            pstmt.setString(3, oldHash);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update password error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Update user's full name.
     */
    public boolean updateFullName(int userId, String newFullName) {
        String sql = "UPDATE users SET full_name = ? WHERE id = ?";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, newFullName.trim());
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Update name error: " + e.getMessage());
        }
        return false;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setCreatedAt(rs.getString("created_at"));
        return user;
    }

    /**
     * Hash password using SHA-256.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
