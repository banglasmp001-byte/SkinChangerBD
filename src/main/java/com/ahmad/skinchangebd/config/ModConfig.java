package com.ahmad.skinchangebd.config;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent JSON configuration for SkinChangerBD.
 * Stored at: .minecraft/config/skinchangebd.json
 *
 * Created by Ahmad
 */
public class ModConfig {

    private static final ModConfig INSTANCE = new ModConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;

    // ── Settings ──────────────────────────────────────────────────────────────
    private boolean enabled           = true;
    private boolean skinEnabled       = true;
    private boolean capeEnabled       = true;
    private boolean multiplayerSync   = true;
    private boolean autoRefresh       = false;     // filesystem watch; conservative default
    private boolean previewEnabled    = true;
    private String  selectedSkin      = "";
    private String  selectedCape      = "";
    private String  skinModelType     = "classic"; // "classic" or "slim"
    private int     cacheMaxMb        = 50;        // max MB for texture cache

    private ModConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("skinchangebd.json");
    }

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public void load() {
        if (!Files.exists(configPath)) {
            save(); // write defaults
            return;
        }
        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            enabled         = getBoolean(obj, "enabled",         enabled);
            skinEnabled     = getBoolean(obj, "skinEnabled",     skinEnabled);
            capeEnabled     = getBoolean(obj, "capeEnabled",     capeEnabled);
            multiplayerSync = getBoolean(obj, "multiplayerSync", multiplayerSync);
            autoRefresh     = getBoolean(obj, "autoRefresh",     autoRefresh);
            previewEnabled  = getBoolean(obj, "previewEnabled",  previewEnabled);
            selectedSkin    = getString (obj, "selectedSkin",    selectedSkin);
            selectedCape    = getString (obj, "selectedCape",    selectedCape);
            skinModelType   = getString (obj, "skinModelType",   skinModelType);
            cacheMaxMb      = getInt    (obj, "cacheMaxMb",      cacheMaxMb);

        } catch (Exception e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to load config, using defaults: {}", e.getMessage());
            save(); // overwrite corrupted config
        }
    }

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("enabled",         enabled);
            obj.addProperty("skinEnabled",     skinEnabled);
            obj.addProperty("capeEnabled",     capeEnabled);
            obj.addProperty("multiplayerSync", multiplayerSync);
            obj.addProperty("autoRefresh",     autoRefresh);
            obj.addProperty("previewEnabled",  previewEnabled);
            obj.addProperty("selectedSkin",    selectedSkin);
            obj.addProperty("selectedCape",    selectedCape);
            obj.addProperty("skinModelType",   skinModelType);
            obj.addProperty("cacheMaxMb",      cacheMaxMb);

            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to save config: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean getBoolean(JsonObject obj, String key, boolean def) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : def;
    }

    private String getString(JsonObject obj, String key, String def) {
        return obj.has(key) ? obj.get(key).getAsString() : def;
    }

    private int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) ? obj.get(key).getAsInt() : def;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public boolean isEnabled()           { return enabled; }
    public void    setEnabled(boolean v)           { this.enabled = v; }

    public boolean isSkinEnabled()       { return skinEnabled; }
    public void    setSkinEnabled(boolean v)       { this.skinEnabled = v; }

    public boolean isCapeEnabled()       { return capeEnabled; }
    public void    setCapeEnabled(boolean v)       { this.capeEnabled = v; }

    public boolean isMultiplayerSync()   { return multiplayerSync; }
    public void    setMultiplayerSync(boolean v)   { this.multiplayerSync = v; }

    public boolean isAutoRefresh()       { return autoRefresh; }
    public void    setAutoRefresh(boolean v)       { this.autoRefresh = v; }

    public boolean isPreviewEnabled()    { return previewEnabled; }
    public void    setPreviewEnabled(boolean v)    { this.previewEnabled = v; }

    public String  getSelectedSkin()     { return selectedSkin; }
    public void    setSelectedSkin(String v)       { this.selectedSkin = v == null ? "" : v; }

    public String  getSelectedCape()     { return selectedCape; }
    public void    setSelectedCape(String v)       { this.selectedCape = v == null ? "" : v; }

    public String  getSkinModelType()    { return skinModelType; }
    public void    setSkinModelType(String v)      { this.skinModelType = v == null ? "classic" : v; }

    public boolean isSlimModel()         { return "slim".equalsIgnoreCase(skinModelType); }

    public int     getCacheMaxMb()       { return cacheMaxMb; }
    public void    setCacheMaxMb(int v)            { this.cacheMaxMb = Math.max(1, v); }
}
