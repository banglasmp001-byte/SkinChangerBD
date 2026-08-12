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

public class SkinChangerBD implements ModInitializer {

    public static final String MOD_ID   = "skinchangebd";
    public static final String MOD_NAME = "SkinChangerBD";
    public static final Logger LOGGER   = LoggerFactory.getLogger(MOD_NAME);

    public static Path SKIN_CHANGER_DIR;
    public static Path SKIN_DIR;
    public static Path CAPE_DIR;
    public static Path CACHE_DIR;
    public static Path CACHE_SKIN_DIR;
    public static Path CACHE_CAPE_DIR;

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Initializing — Created by Ahmad", MOD_NAME);

        Path gameDir      = FabricLoader.getInstance().getGameDir();
        SKIN_CHANGER_DIR  = gameDir.resolve("SkinChangerBD");
        SKIN_DIR          = SKIN_CHANGER_DIR.resolve("skin");
        CAPE_DIR          = SKIN_CHANGER_DIR.resolve("cape");
        CACHE_DIR         = SKIN_CHANGER_DIR.resolve("cache");
        CACHE_SKIN_DIR    = CACHE_DIR.resolve("skin");
        CACHE_CAPE_DIR    = CACHE_DIR.resolve("cape");

        createDirectories();
        ModConfig.getInstance().load();
        PayloadTypeRegistrar.registerAll();
        SkinChangerNetworking.registerServerPackets();
        SkinSyncServer.getInstance().initialize();

        // Background thread এ load করো — main thread block হবে না
        Thread cacheLoader = new Thread(() -> {
            SkinSyncServer.getInstance().loadPersistedTextures();
            LOGGER.info("[{}] Background cache load complete", MOD_NAME);
        }, "SkinChangerBD-CacheLoader");
        cacheLoader.setDaemon(true);
        cacheLoader.start();

        LOGGER.info("[{}] Initialization complete", MOD_NAME);
    }

    private void createDirectories() {
        try {
            Files.createDirectories(SKIN_DIR);
            Files.createDirectories(CAPE_DIR);
            Files.createDirectories(CACHE_SKIN_DIR);
            Files.createDirectories(CACHE_CAPE_DIR);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create directories: {}", MOD_NAME, e.getMessage());
        }
    }
}
