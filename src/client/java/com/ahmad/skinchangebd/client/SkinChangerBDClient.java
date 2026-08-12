package com.ahmad.skinchangebd.client;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.cape.CapeManager;
import com.ahmad.skinchangebd.client.keybind.KeybindManager;
import com.ahmad.skinchangebd.network.ClientNetworkHandler;
import com.ahmad.skinchangebd.skin.SkinManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Client-side entrypoint for SkinChangerBD.
 * Handles: keybind registration, GUI, client networking, skin/cape scanning.
 *
 * Created by Ahmad
 */
public class SkinChangerBDClient implements ClientModInitializer {

    private static SkinChangerBDClient INSTANCE;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        SkinChangerBD.LOGGER.info("[SkinChangerBD] Client initializing...");

        // Register keybinds
        KeybindManager.register();

        // Register client-side network handlers
        ClientNetworkHandler.register();

        // Scan skin/cape directories on startup (off-thread to avoid blocking)
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            Thread scanThread = new Thread(() -> {
                SkinManager.getInstance().refresh();
                CapeManager.getInstance().refresh();
                SkinChangerBD.LOGGER.info("[SkinChangerBD] Initial skin/cape scan complete");
            }, "SkinChangerBD-Scan");
            scanThread.setDaemon(true);
            scanThread.start();
        });

        // Handle keybind presses each tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            KeybindManager.handleTick(client);
        });

        SkinChangerBD.LOGGER.info("[SkinChangerBD] Client initialization complete");
    }

    public static SkinChangerBDClient getInstance() {
        return INSTANCE;
    }
}
