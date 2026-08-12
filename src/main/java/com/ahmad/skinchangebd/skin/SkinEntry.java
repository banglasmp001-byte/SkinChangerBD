package com.ahmad.skinchangebd.skin;

import java.nio.file.Path;

/**
 * Represents a skin PNG file discovered in the SkinChangerBD/skin/ directory.
 *
 * Created by Ahmad
 */
public record SkinEntry(
        /** Display name (filename without extension) */
        String name,
        /** Absolute path to the PNG file */
        Path   path,
        /** "classic" or "slim" */
        String modelType,
        /** SHA-256 hash of the file, cached for comparison */
        String hash
) {
    /** Returns the filename including .png */
    public String filename() {
        return path.getFileName().toString();
    }

    public boolean isSlim() {
        return "slim".equalsIgnoreCase(modelType);
    }
}
