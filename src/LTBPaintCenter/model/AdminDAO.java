package LTBPaintCenter.model;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class AdminDAO {

    // ===== Public API =====

    public static boolean verifyPassword(String plain) {
        try (Connection conn = Database.getConnection()) {
            String[] hs = getHashAndSalt(conn);
            if (hs == null) return false;
            String expectedHash = hs[0];
            String salt = hs[1];
            String actualHash = hashPassword(plain, salt);
            return constantTimeEquals(expectedHash, actualHash);
        } catch (Exception e) {
            System.err.println("[AdminDAO] verifyPassword failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean changePassword(String currentPlain, String newPlain) {
        try (Connection conn = Database.getConnection()) {
            // Verify current
            String[] hs = getHashAndSalt(conn);
            if (hs == null) return false;
            String expectedHash = hs[0];
            String salt = hs[1];
            String actualHash = hashPassword(currentPlain, salt);
            if (!constantTimeEquals(expectedHash, actualHash)) {
                return false;
            }
            // Update with new
            String newSalt = generateSalt();
            String newHash = hashPassword(newPlain, newSalt);
            setHashAndSalt(conn, newHash, newSalt);
            return true;
        } catch (Exception e) {
            System.err.println("[AdminDAO] changePassword failed: " + e.getMessage());
            return false;
        }
    }

    // ===== Internal DB helpers =====

    static String[] getHashAndSalt(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT password_hash, salt FROM admin_settings WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new String[]{rs.getString(1), rs.getString(2)};
            }
        } catch (Exception e) {
            System.err.println("[AdminDAO] getHashAndSalt error: " + e.getMessage());
        }
        return null;
    }

    static void setHashAndSalt(Connection conn, String hash, String salt) {
        try (PreparedStatement up = conn.prepareStatement("UPDATE admin_settings SET password_hash=?, salt=? WHERE id=1")) {
            up.setString(1, hash);
            up.setString(2, salt);
            int n = up.executeUpdate();
            if (n == 0) {
                try (PreparedStatement ins = conn.prepareStatement("INSERT OR REPLACE INTO admin_settings (id, password_hash, salt) VALUES (1, ?, ?)")) {
                    ins.setString(1, hash);
                    ins.setString(2, salt);
                    ins.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminDAO] setHashAndSalt error: " + e.getMessage());
        }
    }

    // ===== Crypto helpers =====

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return toHex(salt);
    }

    public static String hashPassword(String plain, String hexSalt) {
        try {
            byte[] salt = fromHex(hexSalt);
            byte[] data = new byte[salt.length + (plain != null ? plain.getBytes(java.nio.charset.StandardCharsets.UTF_8).length : 0)];
            System.arraycopy(salt, 0, data, 0, salt.length);
            if (plain != null) {
                byte[] pw = plain.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                System.arraycopy(pw, 0, data, salt.length, pw.length);
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return toHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int res = 0;
        for (int i = 0; i < a.length(); i++) res |= a.charAt(i) ^ b.charAt(i);
        return res == 0;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte by : bytes) sb.append(String.format("%02x", by));
        return sb.toString();
        
    }

    private static byte[] fromHex(String s) {
        if (s == null) return new byte[0];
        int len = s.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        return out;
    }
}
