package com.ahmad.skinchangebd.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 hashing utilities for texture identification and cache keying.
 * Ensures the same texture is never transmitted or stored twice.
 *
 * Created by Ahmad
 */
public final class TextureHash {

    private TextureHash() {}

    /**
     * Computes the SHA-256 hex hash of a file.
     *
     * @param path the file to hash
     * @return lowercase hex SHA-256 string, or null on error
     */
    public static String hashFile(Path path) {
        if (path == null || !Files.exists(path)) return null;
        try (InputStream is = Files.newInputStream(path)) {
            return hashStream(is);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Computes the SHA-256 hex hash of a byte array.
     */
    public static String hashBytes(byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return null; // SHA-256 is always available in Java
        }
    }

    private static String hashStream(InputStream is) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int read;
            while ((read = is.read(buf)) != -1) {
                md.update(buf, 0, read);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
