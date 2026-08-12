package com.ahmad.skinchangebd.util;

import com.ahmad.skinchangebd.SkinChangerBD;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates PNG textures for skin/cape use.
 * Prevents loading of invalid, oversized, or malicious image files.
 *
 * Created by Ahmad
 */
public final class TextureValidator {

    /** Maximum skin file size in bytes (256 KB) */
    public static final long MAX_SKIN_BYTES = 256 * 1024L;
    /** Maximum cape file size in bytes (256 KB) */
    public static final long MAX_CAPE_BYTES = 256 * 1024L;

    // Valid skin dimensions
    private static final int SKIN_W_LEGACY = 64;
    private static final int SKIN_H_LEGACY = 32;
    private static final int SKIN_W        = 64;
    private static final int SKIN_H        = 64;

    // Valid cape dimensions (standard Mojang: 64×32, Optifine: 46×22 also common)
    private static final int CAPE_W = 64;
    private static final int CAPE_H = 32;
    private static final int CAPE_W_ALT = 46;
    private static final int CAPE_H_ALT = 22;

    private TextureValidator() {}

    /**
     * Validates a PNG file as a Minecraft skin.
     *
     * @param path the file to validate
     * @return validation result with error message on failure
     */
    public static ValidationResult validateSkin(Path path) {
        if (path == null || !Files.exists(path)) {
            return ValidationResult.failure("File does not exist");
        }

        // Check file size
        try {
            long size = Files.size(path);
            if (size == 0) return ValidationResult.failure("File is empty");
            if (size > MAX_SKIN_BYTES) return ValidationResult.failure(
                    "File too large: " + size + " bytes (max " + MAX_SKIN_BYTES + ")");
        } catch (IOException e) {
            return ValidationResult.failure("Cannot read file size: " + e.getMessage());
        }

        // Check PNG header bytes
        if (!isPngFile(path)) {
            return ValidationResult.failure("Not a valid PNG file");
        }

        // Load and check image dimensions
        try (InputStream is = Files.newInputStream(path)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) return ValidationResult.failure("Cannot parse PNG image");

            int w = img.getWidth();
            int h = img.getHeight();

            boolean validDimensions =
                    (w == SKIN_W && h == SKIN_H) ||          // modern 64×64
                    (w == SKIN_W_LEGACY && h == SKIN_H_LEGACY); // legacy 64×32

            if (!validDimensions) {
                return ValidationResult.failure(
                        "Invalid skin dimensions: " + w + "x" + h +
                        " (expected 64x64 or 64x32)");
            }

            return ValidationResult.success(w, h);
        } catch (Exception e) {
            return ValidationResult.failure("Error reading PNG: " + e.getMessage());
        }
    }

    /**
     * Validates a PNG file as a Minecraft cape.
     */
    public static ValidationResult validateCape(Path path) {
        if (path == null || !Files.exists(path)) {
            return ValidationResult.failure("File does not exist");
        }

        try {
            long size = Files.size(path);
            if (size == 0) return ValidationResult.failure("File is empty");
            if (size > MAX_CAPE_BYTES) return ValidationResult.failure(
                    "File too large: " + size + " bytes (max " + MAX_CAPE_BYTES + ")");
        } catch (IOException e) {
            return ValidationResult.failure("Cannot read file size: " + e.getMessage());
        }

        if (!isPngFile(path)) {
            return ValidationResult.failure("Not a valid PNG file");
        }

        try (InputStream is = Files.newInputStream(path)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) return ValidationResult.failure("Cannot parse PNG image");

            int w = img.getWidth();
            int h = img.getHeight();

            boolean validDimensions =
                    (w == CAPE_W && h == CAPE_H) ||
                    (w == CAPE_W_ALT && h == CAPE_H_ALT);

            if (!validDimensions) {
                return ValidationResult.failure(
                        "Invalid cape dimensions: " + w + "x" + h +
                        " (expected 64x32 or 46x22)");
            }

            return ValidationResult.success(w, h);
        } catch (Exception e) {
            return ValidationResult.failure("Error reading PNG: " + e.getMessage());
        }
    }

    /**
     * Validates raw PNG bytes (used for network-received textures).
     */
    public static ValidationResult validateSkinBytes(byte[] data) {
        if (data == null || data.length == 0) return ValidationResult.failure("Empty data");
        if (data.length > MAX_SKIN_BYTES) return ValidationResult.failure("Data too large");
        if (!isPngBytes(data)) return ValidationResult.failure("Not PNG data");

        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
            BufferedImage img = ImageIO.read(bais);
            if (img == null) return ValidationResult.failure("Cannot parse PNG");
            int w = img.getWidth(), h = img.getHeight();
            if (!((w == 64 && h == 64) || (w == 64 && h == 32))) {
                return ValidationResult.failure("Invalid skin dimensions: " + w + "x" + h);
            }
            return ValidationResult.success(w, h);
        } catch (Exception e) {
            return ValidationResult.failure("Parse error: " + e.getMessage());
        }
    }

    public static ValidationResult validateCapeBytes(byte[] data) {
        if (data == null || data.length == 0) return ValidationResult.failure("Empty data");
        if (data.length > MAX_CAPE_BYTES) return ValidationResult.failure("Data too large");
        if (!isPngBytes(data)) return ValidationResult.failure("Not PNG data");

        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
            BufferedImage img = ImageIO.read(bais);
            if (img == null) return ValidationResult.failure("Cannot parse PNG");
            int w = img.getWidth(), h = img.getHeight();
            if (!((w == 64 && h == 32) || (w == 46 && h == 22))) {
                return ValidationResult.failure("Invalid cape dimensions: " + w + "x" + h);
            }
            return ValidationResult.success(w, h);
        } catch (Exception e) {
            return ValidationResult.failure("Parse error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Check PNG magic bytes: 0x89 PNG\r\n\x1a\n */
    private static boolean isPngFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] header = new byte[8];
            if (is.read(header) < 8) return false;
            return isPngBytes(header);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isPngBytes(byte[] data) {
        if (data.length < 8) return false;
        return (data[0] == (byte) 0x89) &&
               (data[1] == 0x50) && // P
               (data[2] == 0x4E) && // N
               (data[3] == 0x47) && // G
               (data[4] == 0x0D) &&
               (data[5] == 0x0A) &&
               (data[6] == 0x1A) &&
               (data[7] == 0x0A);
    }

    /**
     * Sanitizes a filename to prevent path traversal attacks.
     * Returns null if the filename is unsafe.
     */
    public static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return null;
        // Remove all path separators and dangerous characters
        String safe = name.replaceAll("[/\\\\:*?\"<>|]", "_");
        // Prevent hidden files or dot-only names
        safe = safe.replaceAll("^\\.+", "_");
        // Ensure .png extension
        if (!safe.toLowerCase().endsWith(".png")) safe += ".png";
        // Limit length
        if (safe.length() > 128) safe = safe.substring(0, 120) + ".png";
        return safe;
    }

    // ── Result record ─────────────────────────────────────────────────────────

    public record ValidationResult(boolean valid, String error, int width, int height) {
        public static ValidationResult success(int w, int h) { return new ValidationResult(true, null, w, h); }
        public static ValidationResult failure(String msg)   { return new ValidationResult(false, msg, 0, 0); }
    }
}
