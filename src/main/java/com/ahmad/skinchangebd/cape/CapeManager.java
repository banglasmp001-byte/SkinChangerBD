package com.ahmad.skinchangebd.cape;

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
 * Manages the local cape library (scan, import, delete, select, apply).
 * Mirrors the design of {@link com.ahmad.skinchangebd.skin.SkinManager}.
 *
 * Created by Ahmad
 */
public class CapeManager {

    private static final CapeManager INSTANCE = new CapeManager();

    private final List<CapeEntry> entries = new ArrayList<>();
    private CapeEntry activeEntry = null;

    private CapeManager() {}

    public static CapeManager getInstance() {
        return INSTANCE;
    }

    // ── Library scan ─────────────────────────────────────────────────────────

    public synchronized void refresh() {
        List<CapeEntry> found = new ArrayList<>();
        Path capeDir = SkinChangerBD.CAPE_DIR;

        if (!Files.isDirectory(capeDir)) {
            entries.clear();
            return;
        }

        try (Stream<Path> stream = Files.list(capeDir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                  .sorted()
                  .forEach(p -> {
                      TextureValidator.ValidationResult vr = TextureValidator.validateCape(p);
                      if (vr.valid()) {
                          String name = stripExtension(p.getFileName().toString());
                          String hash = TextureHash.hashFile(p);
                          found.add(new CapeEntry(name, p, hash));
                      } else {
                          SkinChangerBD.LOGGER.warn("[SkinChangerBD] Skipping invalid cape {}: {}", p, vr.error());
                      }
                  });
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Error scanning cape directory: {}", e.getMessage());
        }

        entries.clear();
        entries.addAll(found);

        String selectedName = ModConfig.getInstance().getSelectedCape();
        if (!selectedName.isBlank()) {
            activeEntry = entries.stream()
                    .filter(e -> e.name().equals(selectedName))
                    .findFirst()
                    .orElse(null);
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    public synchronized CapeEntry importCape(Path sourcePath) {
        TextureValidator.ValidationResult vr = TextureValidator.validateCape(sourcePath);
        if (!vr.valid()) {
            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Cannot import cape: {}", vr.error());
            return null;
        }

        String rawName  = sourcePath.getFileName().toString();
        String safeName = TextureValidator.sanitizeFilename(rawName);
        if (safeName == null) return null;

        Path dest = resolveUniquePath(SkinChangerBD.CAPE_DIR, safeName);

        try {
            Files.copy(sourcePath, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to copy cape: {}", e.getMessage());
            return null;
        }

        String name  = stripExtension(dest.getFileName().toString());
        String hash  = TextureHash.hashFile(dest);
        CapeEntry entry = new CapeEntry(name, dest, hash);
        entries.add(entry);
        return entry;
    }

    public synchronized boolean saveCapeBytes(byte[] data, String filename) {
        TextureValidator.ValidationResult vr = TextureValidator.validateCapeBytes(data);
        if (!vr.valid()) return false;
        String safeName = TextureValidator.sanitizeFilename(filename);
        if (safeName == null) return false;
        Path dest = SkinChangerBD.CACHE_CAPE_DIR.resolve(safeName);
        try {
            Files.write(dest, data);
            return true;
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to save cached cape: {}", e.getMessage());
            return false;
        }
    }

    // ── Select / Reset ────────────────────────────────────────────────────────

    public synchronized void selectCape(CapeEntry entry) {
        this.activeEntry = entry;
        ModConfig cfg = ModConfig.getInstance();
        cfg.setSelectedCape(entry != null ? entry.name() : "");
        cfg.save();
    }

    public synchronized CapeEntry getActiveCape() {
        return activeEntry;
    }

    public synchronized void resetCape() {
        activeEntry = null;
        ModConfig.getInstance().setSelectedCape("");
        ModConfig.getInstance().save();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public synchronized boolean deleteCape(CapeEntry entry) {
        try {
            Files.deleteIfExists(entry.path());
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to delete cape {}: {}", entry.path(), e.getMessage());
            return false;
        }
        entries.remove(entry);
        if (entry.equals(activeEntry)) {
            activeEntry = null;
            ModConfig.getInstance().setSelectedCape("");
            ModConfig.getInstance().save();
        }
        return true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public synchronized List<CapeEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static Path resolveUniquePath(Path dir, String filename) {
        Path candidate = dir.resolve(filename);
        if (!Files.exists(candidate)) return candidate;
        String name = stripExtension(filename);
        int i = 1;
        do {
            candidate = dir.resolve(name + "_" + i + ".png");
            i++;
        } while (Files.exists(candidate));
        return candidate;
    }
}
