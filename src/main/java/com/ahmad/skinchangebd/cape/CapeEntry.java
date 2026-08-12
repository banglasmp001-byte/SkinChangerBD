package com.ahmad.skinchangebd.cape;

import java.nio.file.Path;

/**
 * Represents a cape PNG file in the SkinChangerBD/cape/ directory.
 *
 * Created by Ahmad
 */
public record CapeEntry(
        /** Display name (filename without extension) */
        String name,
        /** Absolute path to the PNG file */
        Path   path,
        /** SHA-256 hash of the file */
        String hash
) {
    public String filename() {
        return path.getFileName().toString();
    }
}
