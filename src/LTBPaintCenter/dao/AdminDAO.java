package LTBPaintCenter.dao;

import LTBPaintCenter.model.Database;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * This class handles admin password management.
 * It stores passwords securely using hashing and salt.
 * Passwords are never stored in plain text - only their hashes are stored.
 */
public class AdminDAO {

    /**
     * Verifies if a password is correct.
     * Compares the provided password with the stored hash.
     * 
     * @param plainPassword The password to verify (in plain text)
     * @return true if password is correct, false otherwise
     */
    public static boolean verifyPassword(String plainPassword) {
        try (Connection conn = Database.getConnection()) {
            String[] hashAndSalt = getHashAndSalt(conn);
            if (hashAndSalt == null) {
                return false;
            }
            
            String expectedHash = hashAndSalt[0];
            String salt = hashAndSalt[1];
            String actualHash = hashPassword(plainPassword, salt);
            
            // Use constant-time comparison to prevent timing attacks
            return constantTimeEquals(expectedHash, actualHash);
        } catch (Exception e) {
            System.err.println("[AdminDAO] verifyPassword failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Changes the admin password.
     * Requires the current password to be verified first.
     * 
     * @param currentPassword The current password
     * @param newPassword The new password to set
     * @return true if password was changed successfully, false otherwise
     */
    public static boolean changePassword(String currentPassword, String newPassword) {
        try (Connection conn = Database.getConnection()) {
            // First verify the current password
            String[] hashAndSalt = getHashAndSalt(conn);
            if (hashAndSalt == null) {
                return false;
            }
            
            String expectedHash = hashAndSalt[0];
            String salt = hashAndSalt[1];
            String actualHash = hashPassword(currentPassword, salt);
            
            if (!constantTimeEquals(expectedHash, actualHash)) {
                return false;  // Current password is wrong
            }
            
            // Generate new salt and hash for the new password
            String newSalt = generateSalt();
            String newHash = hashPassword(newPassword, newSalt);
            setHashAndSalt(conn, newHash, newSalt);
            
            return true;
        } catch (Exception e) {
            System.err.println("[AdminDAO] changePassword failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the password hash and salt from the database.
     * 
     * @param conn The database connection
     * @return An array with [hash, salt], or null if not found
     */
    static String[] getHashAndSalt(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT password_hash, salt FROM admin_settings WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return new String[]{rs.getString(1), rs.getString(2)};
            }
        } catch (Exception e) {
            System.err.println("[AdminDAO] getHashAndSalt error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the password hash and salt in the database.
     * 
     * @param conn The database connection
     * @param hash The password hash
     * @param salt The salt used for hashing
     */
    static void setHashAndSalt(Connection conn, String hash, String salt) {
        try (PreparedStatement update = conn.prepareStatement(
                "UPDATE admin_settings SET password_hash=?, salt=? WHERE id=1")) {
            update.setString(1, hash);
            update.setString(2, salt);
            int rowsAffected = update.executeUpdate();
            
            // If no row was updated, insert a new one
            if (rowsAffected == 0) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT OR REPLACE INTO admin_settings (id, password_hash, salt) VALUES (1, ?, ?)")) {
                    insert.setString(1, hash);
                    insert.setString(2, salt);
                    insert.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminDAO] setHashAndSalt error: " + e.getMessage());
        }
    }

    /**
     * Generates a random salt for password hashing.
     * Salt makes passwords more secure by adding randomness.
     * 
     * @return A hexadecimal string representing the salt
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return toHex(salt);
    }

    /**
     * Hashes a password with a salt using SHA-256.
     * This is a one-way function - you can't get the password back from the hash.
     * 
     * @param plainPassword The password in plain text
     * @param hexSalt The salt in hexadecimal format
     * @return The hashed password as a hexadecimal string
     */
    public static String hashPassword(String plainPassword, String hexSalt) {
        try {
            byte[] salt = fromHex(hexSalt);
            byte[] passwordBytes = plainPassword != null ? 
                    plainPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8) : 
                    new byte[0];
            
            // Combine salt and password
            byte[] data = new byte[salt.length + passwordBytes.length];
            System.arraycopy(salt, 0, data, 0, salt.length);
            System.arraycopy(passwordBytes, 0, data, salt.length, passwordBytes.length);
            
            // Hash using SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            
            return toHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Compares two strings in constant time.
     * This prevents timing attacks where an attacker could guess
     * the password by measuring how long the comparison takes.
     * 
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Converts a byte array to a hexadecimal string.
     * 
     * @param bytes The byte array to convert
     * @return A hexadecimal string
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Converts a hexadecimal string to a byte array.
     * 
     * @param hexString The hexadecimal string to convert
     * @return A byte array
     */
    private static byte[] fromHex(String hexString) {
        if (hexString == null) {
            return new byte[0];
        }
        
        int length = hexString.length();
        byte[] output = new byte[length / 2];
        
        for (int i = 0; i < length; i += 2) {
            output[i / 2] = (byte) Integer.parseInt(hexString.substring(i, i + 2), 16);
        }
        
        return output;
    }
}
