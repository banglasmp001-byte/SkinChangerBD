package com.ahmad.skinchangebd.network;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.cape.CapeManager;
import com.ahmad.skinchangebd.config.ModConfig;
import com.ahmad.skinchangebd.render.SkinTextureManager;
import com.ahmad.skinchangebd.skin.SkinManager;
import com.ahmad.skinchangebd.util.TextureHash;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * Registers all client-side packet handlers and manages outbound sync packets.
 *
 * Created by Ahmad
 */
public final class ClientNetworkHandler {

    /** True when the server has SkinChangerBD installed and sync is possible */
    private static boolean serverSupportsSkin = false;

    private ClientNetworkHandler() {}

    public static void register() {
        // ── Inbound (S→C) ───────────────────────────────────────────────────

        // Server tells us about another player's skin
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.PlayerSkinInfoPayload.ID,
                (payload, context) -> {
                    String playerName = payload.playerName();
                    String hash       = payload.skinHash();
                    String model      = payload.modelType();

                    context.client().execute(() -> {
                        SkinTextureManager mgr = SkinTextureManager.getInstance();
                        if (mgr.hasSkin(hash)) {
                            // Already cached locally — apply immediately
                            mgr.setPlayerSkin(playerName, hash, model);
                        } else {
                            // Need to download — server will send data next,
                            // or we can request it. Mark as pending.
                            mgr.setPendingSkin(playerName, hash, model);
                            // Ask server to send the data
                            requestSkinData(hash);
                        }
                    });
                }
        );

        // Server tells us about another player's cape
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.PlayerCapeInfoPayload.ID,
                (payload, context) -> {
                    String playerName = payload.playerName();
                    String hash       = payload.capeHash();

                    context.client().execute(() -> {
                        SkinTextureManager mgr = SkinTextureManager.getInstance();
                        if (mgr.hasCape(hash)) {
                            mgr.setPlayerCape(playerName, hash);
                        } else {
                            mgr.setPendingCape(playerName, hash);
                            requestCapeData(hash);
                        }
                    });
                }
        );

        // Server asks us to upload our skin PNG
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.RequestSkinUploadPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        uploadCurrentSkin();
                    });
                }
        );

        // Server asks us to upload our cape PNG
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.RequestCapeUploadPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        uploadCurrentCape();
                    });
                }
        );

        // Server sends us another player's skin PNG
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.ServerSkinDataPayload.ID,
                (payload, context) -> {
                    String hash   = payload.hash();
                    byte[] data   = payload.data();
                    String pName  = payload.playerName();

                    context.client().execute(() -> {
                        SkinTextureManager mgr = SkinTextureManager.getInstance();
                        mgr.cacheSkinBytes(hash, data);
                        if (!pName.isBlank()) {
                            mgr.setPlayerSkin(pName, hash, "classic"); // model set separately
                        }
                    });
                }
        );

        // Server sends us another player's cape PNG
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.ServerCapeDataPayload.ID,
                (payload, context) -> {
                    String hash  = payload.hash();
                    byte[] data  = payload.data();
                    String pName = payload.playerName();

                    context.client().execute(() -> {
                        SkinTextureManager mgr = SkinTextureManager.getInstance();
                        mgr.cacheCapeBytes(hash, data);
                        if (!pName.isBlank()) {
                            mgr.setPlayerCape(pName, hash);
                        }
                    });
                }
        );

        // Server broadcasts that a player reset their skin
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.PlayerResetSkinPayload.ID,
                (payload, context) -> {
                    context.client().execute(() ->
                        SkinTextureManager.getInstance().clearPlayerSkin(payload.playerName())
                    );
                }
        );

        // Server broadcasts that a player reset their cape
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.PlayerResetCapePayload.ID,
                (payload, context) -> {
                    context.client().execute(() ->
                        SkinTextureManager.getInstance().clearPlayerCape(payload.playerName())
                    );
                }
        );

        // Server announces it supports SkinChangerBD
        ClientPlayNetworking.registerGlobalReceiver(
                SkinChangerNetworking.ServerSupportPayload.ID,
                (payload, context) -> {
                    serverSupportsSkin = payload.supported();
                    SkinChangerBD.LOGGER.info("[SkinChangerBD] Server sync support: {}", serverSupportsSkin);
                    if (serverSupportsSkin) {
                        // Announce our own selections to the server
                        context.client().execute(() -> announceCurrentSelections());
                    }
                }
        );

        // ── Connection events ────────────────────────────────────────────────

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverSupportsSkin = false;
            SkinTextureManager.getInstance().clearAllRemoteSkins();
        });

        SkinChangerBD.LOGGER.debug("[SkinChangerBD] Client network handlers registered");
    }

    // ── Outbound helpers ──────────────────────────────────────────────────────

    /**
     * Sends the client's currently selected skin hash to the server.
     * Called when: player applies a skin, or joins a server with SkinChangerBD.
     */
    public static void announceCurrentSelections() {
        if (!serverSupportsSkin) return;
        if (!ModConfig.getInstance().isMultiplayerSync()) return;

        var skinEntry = SkinManager.getInstance().getActiveSkin();
        if (skinEntry != null && skinEntry.hash() != null) {
            ClientPlayNetworking.send(new SkinChangerNetworking.SkinSelectionPayload(
                    skinEntry.hash(), skinEntry.modelType()));
        } else {
            ClientPlayNetworking.send(new SkinChangerNetworking.SkinSelectionPayload("", "classic"));
        }

        var capeEntry = CapeManager.getInstance().getActiveCape();
        if (capeEntry != null && capeEntry.hash() != null) {
            ClientPlayNetworking.send(new SkinChangerNetworking.CapeSelectionPayload(capeEntry.hash()));
        } else {
            ClientPlayNetworking.send(new SkinChangerNetworking.CapeSelectionPayload(""));
        }
    }

    /** Upload current skin PNG data to server */
    private static void uploadCurrentSkin() {
        var entry = SkinManager.getInstance().getActiveSkin();
        if (entry == null) return;
        try {
            byte[] data = java.nio.file.Files.readAllBytes(entry.path());
            ClientPlayNetworking.send(new SkinChangerNetworking.SkinDataPayload(data));
        } catch (java.io.IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to read skin for upload: {}", e.getMessage());
        }
    }

    /** Upload current cape PNG data to server */
    private static void uploadCurrentCape() {
        var entry = CapeManager.getInstance().getActiveCape();
        if (entry == null) return;
        try {
            byte[] data = java.nio.file.Files.readAllBytes(entry.path());
            ClientPlayNetworking.send(new SkinChangerNetworking.CapeDataPayload(data));
        } catch (java.io.IOException e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to read cape for upload: {}", e.getMessage());
        }
    }

    /** Tell server we want a specific skin texture (not yet shown in protocol — for future use) */
    private static void requestSkinData(String hash) {
        // In this protocol, the server proactively sends data with PlayerSkinInfo.
        // This is a no-op hook for future C→S "give me hash X" packet.
    }

    private static void requestCapeData(String hash) {
        // Same as above
    }

    public static boolean isServerSyncAvailable() {
        return serverSupportsSkin;
    }
}
