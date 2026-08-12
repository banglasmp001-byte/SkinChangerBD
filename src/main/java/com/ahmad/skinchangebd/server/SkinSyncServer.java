package com.ahmad.skinchangebd.server;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.network.SkinChangerNetworking;
import com.ahmad.skinchangebd.util.TextureHash;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side skin/cape synchronization engine.
 *
 * Architecture:
 *  1. Player selects skin/cape on client → sends hash to server
 *  2. Server checks if it has the texture cached
 *  3. If not cached → server asks client to upload the PNG
 *  4. Server stores the PNG keyed by SHA-256 hash
 *  5. Server broadcasts skin/cape info (hash + model) to all other players
 *  6. Receiving clients check their local cache → if missing, request the PNG from server
 *  7. Server sends PNG to requesting client
 *
 * Created by Ahmad
 */
public class SkinSyncServer {

    private static final SkinSyncServer INSTANCE = new SkinSyncServer();

    /** Per-player skin state on the server */
    private final Map<UUID, PlayerSkinState> playerStates = new ConcurrentHashMap<>();

    /** Server-side texture cache: hash → PNG bytes */
    private final Map<String, byte[]> skinCache = new ConcurrentHashMap<>();
    private final Map<String, byte[]> capeCache = new ConcurrentHashMap<>();

    private MinecraftServer server;

    private SkinSyncServer() {}

    public static SkinSyncServer getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        // Register lifecycle events
        ServerLifecycleEvents.SERVER_STARTING.register(srv -> this.server = srv);
        ServerLifecycleEvents.SERVER_STOPPED.register(srv -> {
            playerStates.clear();
            // Keep the skin/cape cache — it persists to disk
        });

        // When a player joins, send them info about all other players
        // and broadcast a support packet so they know the server has the mod
        ServerPlayConnectionEvents.JOIN.register((handler, sender, srv) -> {
            ServerPlayerEntity joining = handler.player;
            srv.execute(() -> {
                // Tell the joining player the server supports sync
                if (ServerPlayNetworking.canSend(joining, SkinChangerNetworking.ServerSupportPayload.ID)) {
                    ServerPlayNetworking.send(joining,
                        new SkinChangerNetworking.ServerSupportPayload(true));
                }
                // Send existing players' skin info to the joining player
                sendAllPlayerInfoTo(joining);
                // Broadcast join to others — they don't yet have info for the new player
                // (the new player will announce themselves via C2S_SKIN_SELECTION shortly)
            });
        });

        // Clean up on disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, srv) -> {
            ServerPlayerEntity leaving = handler.player;
            playerStates.remove(leaving.getUuid());
            // Broadcast reset to others
            broadcastSkinReset(leaving);
            broadcastCapeReset(leaving);
        });

        SkinChangerBD.LOGGER.info("[SkinChangerBD] Server sync engine initialized");
    }

    // ── Incoming: player announces skin selection ─────────────────────────────

    public void onPlayerSkinSelection(ServerPlayerEntity player, String skinHash, String modelType) {
        if (skinHash == null || skinHash.isBlank()) {
            // Player reset their skin
            getOrCreateState(player).skinHash = "";
            getOrCreateState(player).skinModelType = "classic";
            broadcastSkinReset(player);
            return;
        }

        PlayerSkinState state = getOrCreateState(player);
        state.skinHash      = skinHash;
        state.skinModelType = modelType;

        if (skinCache.containsKey(skinHash)) {
            // Server already has this texture; broadcast immediately
            broadcastPlayerSkinInfo(player, skinHash, modelType);
        } else {
            // Ask the player to upload the PNG
            if (ServerPlayNetworking.canSend(player, SkinChangerNetworking.RequestSkinUploadPayload.ID)) {
                ServerPlayNetworking.send(player,
                        new SkinChangerNetworking.RequestSkinUploadPayload(skinHash));
            }
        }
    }

    public void onPlayerCapeSelection(ServerPlayerEntity player, String capeHash) {
        if (capeHash == null || capeHash.isBlank()) {
            getOrCreateState(player).capeHash = "";
            broadcastCapeReset(player);
            return;
        }

        PlayerSkinState state = getOrCreateState(player);
        state.capeHash = capeHash;

        if (capeCache.containsKey(capeHash)) {
            broadcastPlayerCapeInfo(player, capeHash);
        } else {
            if (ServerPlayNetworking.canSend(player, SkinChangerNetworking.RequestCapeUploadPayload.ID)) {
                ServerPlayNetworking.send(player,
                        new SkinChangerNetworking.RequestCapeUploadPayload(capeHash));
            }
        }
    }

    // ── Incoming: player uploads texture data ─────────────────────────────────

    public void onSkinDataReceived(ServerPlayerEntity player, byte[] data) {
        String hash = TextureHash.hashBytes(data);
        if (hash == null) return;

        skinCache.put(hash, data);

        // Optional: persist to disk so it survives server restarts
        persistTexture(hash, data, true);

        PlayerSkinState state = getOrCreateState(player);
        if (hash.equals(state.skinHash)) {
            broadcastPlayerSkinInfo(player, hash, state.skinModelType);
        }
    }

    public void onCapeDataReceived(ServerPlayerEntity player, byte[] data) {
        String hash = TextureHash.hashBytes(data);
        if (hash == null) return;

        capeCache.put(hash, data);
        persistTexture(hash, data, false);

        PlayerSkinState state = getOrCreateState(player);
        if (hash.equals(state.capeHash)) {
            broadcastPlayerCapeInfo(player, hash);
        }
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────

    private void broadcastPlayerSkinInfo(ServerPlayerEntity source, String hash, String modelType) {
        if (server == null) return;
        String name = source.getNameForScoreboard();

        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other == source) continue;
            if (!ServerPlayNetworking.canSend(other, SkinChangerNetworking.PlayerSkinInfoPayload.ID)) continue;

            ServerPlayNetworking.send(other,
                    new SkinChangerNetworking.PlayerSkinInfoPayload(name, hash, modelType));
        }
    }

    private void broadcastPlayerCapeInfo(ServerPlayerEntity source, String hash) {
        if (server == null) return;
        String name = source.getNameForScoreboard();

        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other == source) continue;
            if (!ServerPlayNetworking.canSend(other, SkinChangerNetworking.PlayerCapeInfoPayload.ID)) continue;

            ServerPlayNetworking.send(other,
                    new SkinChangerNetworking.PlayerCapeInfoPayload(name, hash));
        }
    }

    private void broadcastSkinReset(ServerPlayerEntity source) {
        if (server == null) return;
        String name = source.getNameForScoreboard();
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other == source) continue;
            if (ServerPlayNetworking.canSend(other, SkinChangerNetworking.PlayerResetSkinPayload.ID)) {
                ServerPlayNetworking.send(other, new SkinChangerNetworking.PlayerResetSkinPayload(name));
            }
        }
    }

    private void broadcastCapeReset(ServerPlayerEntity source) {
        if (server == null) return;
        String name = source.getNameForScoreboard();
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other == source) continue;
            if (ServerPlayNetworking.canSend(other, SkinChangerNetworking.PlayerResetCapePayload.ID)) {
                ServerPlayNetworking.send(other, new SkinChangerNetworking.PlayerResetCapePayload(name));
            }
        }
    }

    /**
     * Sends all currently known player skin/cape info to a newly joined player.
     */
    public void sendAllPlayerInfoTo(ServerPlayerEntity target) {
        if (server == null) return;

        for (Map.Entry<UUID, PlayerSkinState> entry : playerStates.entrySet()) {
            ServerPlayerEntity other = server.getPlayerManager().getPlayer(entry.getKey());
            if (other == null || other == target) continue;

            PlayerSkinState state = entry.getValue();
            String name = other.getNameForScoreboard();

            if (!state.skinHash.isBlank() &&
                    ServerPlayNetworking.canSend(target, SkinChangerNetworking.PlayerSkinInfoPayload.ID)) {
                ServerPlayNetworking.send(target,
                        new SkinChangerNetworking.PlayerSkinInfoPayload(name, state.skinHash, state.skinModelType));
            }

            if (!state.capeHash.isBlank() &&
                    ServerPlayNetworking.canSend(target, SkinChangerNetworking.PlayerCapeInfoPayload.ID)) {
                ServerPlayNetworking.send(target,
                        new SkinChangerNetworking.PlayerCapeInfoPayload(name, state.capeHash));
            }
        }
    }

    // ── Client requests texture data ──────────────────────────────────────────

    /**
     * Called when a client wants the actual PNG data for a skin hash.
     * This would be called via a separate C→S packet not shown here for brevity,
     * but the server sends data proactively on info broadcast.
     */
    public void sendSkinDataToClient(ServerPlayerEntity target, String hash) {
        byte[] data = skinCache.get(hash);
        if (data == null) return;
        if (!ServerPlayNetworking.canSend(target, SkinChangerNetworking.ServerSkinDataPayload.ID)) return;
        ServerPlayNetworking.send(target,
                new SkinChangerNetworking.ServerSkinDataPayload("", hash, true, data));
    }

    public void sendCapeDataToClient(ServerPlayerEntity target, String hash) {
        byte[] data = capeCache.get(hash);
        if (data == null) return;
        if (!ServerPlayNetworking.canSend(target, SkinChangerNetworking.ServerCapeDataPayload.ID)) return;
        ServerPlayNetworking.send(target,
                new SkinChangerNetworking.ServerCapeDataPayload("", hash, data));
    }

    // ── Disk persistence ──────────────────────────────────────────────────────

    private void persistTexture(String hash, byte[] data, boolean isSkin) {
        Path dir  = isSkin ? SkinChangerBD.CACHE_SKIN_DIR : SkinChangerBD.CACHE_CAPE_DIR;
        Path file = dir.resolve(hash + ".png");
        if (Files.exists(file)) return; // already persisted
        try {
            Files.write(file, data);
        } catch (IOException e) {
            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Could not persist texture {}: {}", hash, e.getMessage());
        }
    }

    public void loadPersistedTextures() {
        loadDir(SkinChangerBD.CACHE_SKIN_DIR, skinCache);
        loadDir(SkinChangerBD.CACHE_CAPE_DIR, capeCache);
        SkinChangerBD.LOGGER.info("[SkinChangerBD] Loaded {} cached skins, {} cached capes",
                skinCache.size(), capeCache.size());
    }

    private void loadDir(Path dir, Map<String, byte[]> target) {
        if (!Files.isDirectory(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".png"))
                  .forEach(p -> {
                      try {
                          String name = p.getFileName().toString().replace(".png", "");
                          byte[] data = Files.readAllBytes(p);
                          String hash = TextureHash.hashBytes(data);
                          if (hash != null && hash.equals(name)) {
                              target.put(hash, data);
                          }
                      } catch (IOException ignore) {}
                  });
        } catch (IOException e) {
            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Error loading cached textures: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PlayerSkinState getOrCreateState(ServerPlayerEntity player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerSkinState());
    }

    /** Mutable state stored per-player on the server */
    private static class PlayerSkinState {
        String skinHash      = "";
        String skinModelType = "classic";
        String capeHash      = "";
    }
}
