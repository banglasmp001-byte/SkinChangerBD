package com.ahmad.skinchangebd;

import com.ahmad.skinchangebd.config.ModConfig;
import com.ahmad.skinchangebd.network.PayloadTypeRegistrar;
import com.ahmad.skinchangebd.network.SkinChangerNetworking;
import com.ahmad.skinchangebd.server.SkinSyncServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SkinChangerBD — Main mod entrypoint (common / server side).
 * Handles folder creation, configuration loading, payload registration,
 * and server-side sync engine initialization.
 *
 * Created by Ahmad
 */
public class SkinChangerBD implements ModInitializer {

    public static final String MOD_ID   = "skinchangebd";
    public static final String MOD_NAME = "SkinChangerBD";
    public static final Logger LOGGER   = LoggerFactory.getLogger(MOD_NAME);

    /** Root folder: .minecraft/SkinChangerBD/ */
    public static Path SKIN_CHANGER_DIR;
    /** .minecraft/SkinChangerBD/skin/ */
    public static Path SKIN_DIR;
    /** .minecraft/SkinChangerBD/cape/ */
    public static Path CAPE_DIR;
    /** .minecraft/SkinChangerBD/cache/ */
    public static Path CACHE_DIR;
    /** .minecraft/SkinChangerBD/cache/skin/ */
    public static Path CACHE_SKIN_DIR;
    /** .minecraft/SkinChangerBD/cache/cape/ */
    public static Path CACHE_CAPE_DIR;

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Initializing — Created by Ahmad", MOD_NAME);

        // Resolve directories relative to .minecraft game directory
        Path gameDir      = FabricLoader.getInstance().getGameDir();
        SKIN_CHANGER_DIR  = gameDir.resolve("SkinChangerBD");
        SKIN_DIR          = SKIN_CHANGER_DIR.resolve("skin");
        CAPE_DIR          = SKIN_CHANGER_DIR.resolve("cape");
        CACHE_DIR         = SKIN_CHANGER_DIR.resolve("cache");
        CACHE_SKIN_DIR    = CACHE_DIR.resolve("skin");
        CACHE_CAPE_DIR    = CACHE_DIR.resolve("cape");

        // Create directory structure
        createDirectories();

        // Load configuration
        ModConfig.getInstance().load();

        // Register all custom payload types (required for Fabric 1.21+ networking)
        PayloadTypeRegistrar.registerAll();

        // Register server-side packet handlers
        SkinChangerNetworking.registerServerPackets();

        // Initialize server-side sync engine (registers lifecycle events)
        SkinSyncServer.getInstance().initialize();
        SkinSyncServer.getInstance().loadPersistedTextures();

        LOGGER.info("[{}] Initialization complete", MOD_NAME);
    }

    private void createDirectories() {
        try {
            Files.createDirectories(SKIN_DIR);
            Files.createDirectories(CAPE_DIR);
            Files.createDirectories(CACHE_SKIN_DIR);
            Files.createDirectories(CACHE_CAPE_DIR);
            LOGGER.debug("[{}] Directory structure verified at: {}", MOD_NAME, SKIN_CHANGER_DIR);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create directories at {}: {}", MOD_NAME, SKIN_CHANGER_DIR, e.getMessage());
        }
    }
}
