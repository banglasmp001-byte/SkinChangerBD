package com.ahmad.skinchangebd.skin;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.config.ModConfig;
import com.ahmad.skinchangebd.util.TextureHash;
import com.ahmad.skinchangebd.util.TextureValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages the local skin library (scan, import, delete, apply).
 * Thread-safe reads; writes happen on the calling thread.
 *
 * Created by Ahmad
 */
public class SkinManager {

    private static final SkinManager INSTANCE = new SkinManager();

    private final List<SkinEntry> entries = new ArrayList<>();
    private SkinEntry activeEntry = null;

    private SkinManager() {}

    public static SkinManager getInstance() {
        return INSTANCE;
    }

    // ── Library scan ─────────────────────────────────────────────────────────

    /**
     * Scans the SkinChangerBD/skin/ folder and rebuilds the entry list.
     * Does NOT block the render thread — call from a background thread or on demand.
     */
    public synchronized void refresh() {
        List<SkinEntry> found = new ArrayList<>();
        Path skinDir = SkinChangerBD.SKIN_DIR;

        if (!Files.isDirectory(skinDir)) {
            entries.clear();
            return;
        }

        try (Stream<Path> stream = Files.list(skinDir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                  .sorted()
                  .forEach(p -> {
                      TextureValidator.ValidationResult vr = TextureValidator.validateSkin(p);
                      if (vr.valid()) {
                          String name = stripExtension(p.getFileName().toString());
                          String hash = TextureHash.hashFile(p);
                          String model = ModConfig.getInstance().getSkinModelType();
                          found.add(new SkinEntry(name, p, model, hash));
                      } else {
                          SkinChangerBD.LOGGER.warn("[SkinChangerBD] Skipping invalid skin {}: {}", p, vr.error());
                      }
                  });
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Error scanning skin directory: {}", e.getMessage());
        }

        entries.clear();
        entries.addAll(found);

        // Restore previously selected entry
        String selectedName = ModConfig.getInstance().getSelectedSkin();
        if (!selectedName.isBlank()) {
            activeEntry = entries.stream()
                    .filter(e -> e.name().equals(selectedName))
                    .findFirst()
                    .orElse(null);
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Imports a PNG file from an external path into the SkinChangerBD/skin/ folder.
     *
     * @param sourcePath path to the source PNG
     * @param modelType  "classic" or "slim"
     * @return the imported SkinEntry, or null on failure
     */
    public synchronized SkinEntry importSkin(Path sourcePath, String modelType) {
        TextureValidator.ValidationResult vr = TextureValidator.validateSkin(sourcePath);
        if (!vr.valid()) {
            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Cannot import skin: {}", vr.error());
            return null;
        }

        String rawName = sourcePath.getFileName().toString();
        String safeName = TextureValidator.sanitizeFilename(rawName);
        if (safeName == null) {
            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Unsafe filename rejected: {}", rawName);
            return null;
        }

        Path dest = resolveUniquePath(SkinChangerBD.SKIN_DIR, safeName);

        try {
            Files.copy(sourcePath, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to copy skin: {}", e.getMessage());
            return null;
        }

        String name  = stripExtension(dest.getFileName().toString());
        String hash  = TextureHash.hashFile(dest);
        String model = (modelType != null) ? modelType : "classic";
        SkinEntry entry = new SkinEntry(name, dest, model, hash);
        entries.add(entry);
        return entry;
    }

    /**
     * Saves raw PNG bytes (received from the network cache) into the skin folder.
     */
    public synchronized boolean saveSkinBytes(byte[] data, String filename) {
        TextureValidator.ValidationResult vr = TextureValidator.validateSkinBytes(data);
        if (!vr.valid()) return false;

        String safeName = TextureValidator.sanitizeFilename(filename);
        if (safeName == null) return false;

        Path dest = SkinChangerBD.CACHE_SKIN_DIR.resolve(safeName);
        try {
            Files.write(dest, data);
            return true;
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to save cached skin: {}", e.getMessage());
            return false;
        }
    }

    // ── Select / Apply ────────────────────────────────────────────────────────

    /**
     * Selects a skin as active and persists the selection.
     */
    public synchronized void selectSkin(SkinEntry entry) {
        this.activeEntry = entry;
        ModConfig cfg = ModConfig.getInstance();
        cfg.setSelectedSkin(entry != null ? entry.name() : "");
        if (entry != null) cfg.setSkinModelType(entry.modelType());
        cfg.save();
    }

    /** Returns the currently active (applied) skin, or null if none. */
    public synchronized SkinEntry getActiveSkin() {
        return activeEntry;
    }

    /** Clears the active skin selection. */
    public synchronized void resetSkin() {
        activeEntry = null;
        ModConfig cfg = ModConfig.getInstance();
        cfg.setSelectedSkin("");
        cfg.save();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public synchronized boolean deleteSkin(SkinEntry entry) {
        try {
            Files.deleteIfExists(entry.path());
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to delete skin {}: {}", entry.path(), e.getMessage());
            return false;
        }

        entries.remove(entry);
        if (entry.equals(activeEntry)) {
            activeEntry = null;
            ModConfig.getInstance().setSelectedSkin("");
            ModConfig.getInstance().save();
        }
        return true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public synchronized List<SkinEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    /**
     * If {@code base/name} already exists, appends _1, _2, … until unique.
     */
    private static Path resolveUniquePath(Path dir, String filename) {
        Path candidate = dir.resolve(filename);
        if (!Files.exists(candidate)) return candidate;

        String name = stripExtension(filename);
        String ext  = ".png";
        int i = 1;
        do {
            candidate = dir.resolve(name + "_" + i + ext);
            i++;
        } while (Files.exists(candidate));
        return candidate;
    }
}
