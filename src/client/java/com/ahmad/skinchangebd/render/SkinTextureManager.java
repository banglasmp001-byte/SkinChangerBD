package com.ahmad.skinchangebd.render;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.util.TextureValidator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side texture cache for custom skins and capes.
 *
 * Responsibilities:
 *  - Loads PNG files as Minecraft textures (NativeImageBackedTexture)
 *  - Caches textures by SHA-256 hash to avoid re-loading
 *  - Tracks which player name → which texture hash/Identifier
 *  - Used by the mixin to override player texture lookup
 *
 * Created by Ahmad
 */
public class SkinTextureManager {

    private static final SkinTextureManager INSTANCE = new SkinTextureManager();

    /** hash → Minecraft texture Identifier for skins */
    private final Map<String, Identifier> skinIdentifiers = new ConcurrentHashMap<>();
    /** hash → Minecraft texture Identifier for capes */
    private final Map<String, Identifier> capeIdentifiers = new ConcurrentHashMap<>();

    /** playerName → skin hash (and model type) for remote players */
    private final Map<String, PlayerTextureInfo> playerSkins = new ConcurrentHashMap<>();
    /** playerName → cape hash for remote players */
    private final Map<String, String> playerCapes = new ConcurrentHashMap<>();

    /** Raw byte cache keyed by hash (for textures received but not yet uploaded to GPU) */
    private final Map<String, byte[]> rawSkinBytes = new ConcurrentHashMap<>();
    private final Map<String, byte[]> rawCapeBytes = new ConcurrentHashMap<>();

    /** Our own currently applied skin Identifier */
    private Identifier ownSkinIdentifier = null;
    /** Our own currently applied cape Identifier */
    private Identifier ownCapeIdentifier = null;

    private SkinTextureManager() {}

    public static SkinTextureManager getInstance() {
        return INSTANCE;
    }

    // ── Own skin/cape ─────────────────────────────────────────────────────────

    /**
     * Loads a skin file from disk and registers it as a Minecraft texture.
     * Must be called on the render thread.
     *
     * @return the Identifier of the loaded texture, or null on failure
     */
    public Identifier loadAndApplySkin(Path skinPath) {
        Identifier id = loadTextureFromPath(skinPath, "skin");
        if (id != null) {
            ownSkinIdentifier = id;
        }
        return id;
    }

    public Identifier loadAndApplyCape(Path capePath) {
        Identifier id = loadTextureFromPath(capePath, "cape");
        if (id != null) {
            ownCapeIdentifier = id;
        }
        return id;
    }

    public void clearOwnSkin() {
        ownSkinIdentifier = null;
    }

    public void clearOwnCape() {
        ownCapeIdentifier = null;
    }

    public Identifier getOwnSkinIdentifier() {
        return ownSkinIdentifier;
    }

    public Identifier getOwnCapeIdentifier() {
        return ownCapeIdentifier;
    }

    // ── Remote player skins ───────────────────────────────────────────────────

    public void setPlayerSkin(String playerName, String hash, String modelType) {
        playerSkins.put(playerName, new PlayerTextureInfo(hash, modelType));
        // If we already have the texture loaded, great. If not, try from raw bytes.
        if (!skinIdentifiers.containsKey(hash) && rawSkinBytes.containsKey(hash)) {
            loadTextureFromBytes(rawSkinBytes.get(hash), hash, "skin");
        }
    }

    public void setPendingSkin(String playerName, String hash, String modelType) {
        playerSkins.put(playerName, new PlayerTextureInfo(hash, modelType));
    }

    public void setPlayerCape(String playerName, String hash) {
        playerCapes.put(playerName, hash);
        if (!capeIdentifiers.containsKey(hash) && rawCapeBytes.containsKey(hash)) {
            loadTextureFromBytes(rawCapeBytes.get(hash), hash, "cape");
        }
    }

    public void setPendingCape(String playerName, String hash) {
        playerCapes.put(playerName, hash);
    }

    public void clearPlayerSkin(String playerName) {
        playerSkins.remove(playerName);
    }

    public void clearPlayerCape(String playerName) {
        playerCapes.remove(playerName);
    }

    public void clearAllRemoteSkins() {
        playerSkins.clear();
        playerCapes.clear();
    }

    /** Returns the custom skin Identifier for a player, or null to use default. */
    public Identifier getPlayerSkinIdentifier(String playerName) {
        PlayerTextureInfo info = playerSkins.get(playerName);
        if (info == null) return null;
        return skinIdentifiers.get(info.hash());
    }

    /** Returns the custom cape Identifier for a player, or null to use default. */
    public Identifier getPlayerCapeIdentifier(String playerName) {
        String hash = playerCapes.get(playerName);
        if (hash == null) return null;
        return capeIdentifiers.get(hash);
    }

    /** Returns the model type override for a player ("classic" or "slim"), or null. */
    public String getPlayerModelType(String playerName) {
        PlayerTextureInfo info = playerSkins.get(playerName);
        return info != null ? info.modelType() : null;
    }

    // ── Byte cache ────────────────────────────────────────────────────────────

    public void cacheSkinBytes(String hash, byte[] data) {
        if (hash == null || data == null) return;
        rawSkinBytes.put(hash, data);
        // Immediately upload to GPU if on render thread
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) {
            loadTextureFromBytes(data, hash, "skin");
        }
        // Persist to local disk cache
        saveToDiskCache(hash, data, true);
    }

    public void cacheCapeBytes(String hash, byte[] data) {
        if (hash == null || data == null) return;
        rawCapeBytes.put(hash, data);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) {
            loadTextureFromBytes(data, hash, "cape");
        }
        saveToDiskCache(hash, data, false);
    }

    public boolean hasSkin(String hash) {
        return hash != null && (skinIdentifiers.containsKey(hash) || rawSkinBytes.containsKey(hash));
    }

    public boolean hasCape(String hash) {
        return hash != null && (capeIdentifiers.containsKey(hash) || rawCapeBytes.containsKey(hash));
    }

    // ── Texture loading ───────────────────────────────────────────────────────

    private Identifier loadTextureFromPath(Path path, String type) {
        try {
            byte[] data = Files.readAllBytes(path);
            String hash = com.ahmad.skinchangebd.util.TextureHash.hashBytes(data);
            String typePrefix = "skin".equals(type) ? "skin" : "cape";
            // Check already-registered
            var existing = "skin".equals(type) ? skinIdentifiers.get(hash) : capeIdentifiers.get(hash);
            if (existing != null) return existing;

            return loadTextureFromBytes(data, hash, type);
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to read texture {}: {}", path, e.getMessage());
            return null;
        }
    }

    private Identifier loadTextureFromBytes(byte[] data, String hash, String type) {
        if (data == null || hash == null) return null;

        // Validation
        TextureValidator.ValidationResult vr = "skin".equals(type)
                ? TextureValidator.validateSkinBytes(data)
                : TextureValidator.validateCapeBytes(data);
        if (!vr.valid()) {
            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Rejecting invalid {} texture ({}): {}", type, hash, vr.error());
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            NativeImage image = NativeImage.read(bais);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

            Identifier id = Identifier.of(SkinChangerBD.MOD_ID, type + "/" + hash.substring(0, 16));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);

            if ("skin".equals(type)) {
                skinIdentifiers.put(hash, id);
            } else {
                capeIdentifiers.put(hash, id);
            }

            SkinChangerBD.LOGGER.debug("[SkinChangerBD] Loaded {} texture: {}", type, id);
            return id;
        } catch (Exception e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to create texture from bytes: {}", e.getMessage());
            return null;
        }
    }

    private void saveToDiskCache(String hash, byte[] data, boolean isSkin) {
        Path dir  = isSkin ? SkinChangerBD.CACHE_SKIN_DIR : SkinChangerBD.CACHE_CAPE_DIR;
        Path file = dir.resolve(hash + ".png");
        if (Files.exists(file)) return;
        try {
            Files.write(file, data);
        } catch (IOException e) {
            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Failed to write disk cache: {}", e.getMessage());
        }
    }

    /** Loads any already-cached-on-disk textures from the cache folders. */
    public void loadDiskCache() {
        loadCacheDir(SkinChangerBD.CACHE_SKIN_DIR, "skin");
        loadCacheDir(SkinChangerBD.CACHE_CAPE_DIR, "cape");
    }

    private void loadCacheDir(Path dir, String type) {
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".png"))
                  .forEach(p -> {
                      try {
                          String hash = p.getFileName().toString().replace(".png", "");
                          byte[] data = Files.readAllBytes(p);
                          if ("skin".equals(type)) rawSkinBytes.put(hash, data);
                          else rawCapeBytes.put(hash, data);
                      } catch (IOException ignore) {}
                  });
        } catch (IOException e) {
            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Error reading {} cache: {}", type, e.getMessage());
        }
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private record PlayerTextureInfo(String hash, String modelType) {}
}
