package com.ahmad.skinchangebd.network;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.server.SkinSyncServer;
import com.ahmad.skinchangebd.util.TextureValidator;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Registers and handles server-side custom networking packets.
 * Client-side handlers live in {@link com.ahmad.skinchangebd.network.ClientNetworkHandler}.
 *
 * Created by Ahmad
 */
public final class SkinChangerNetworking {

    private SkinChangerNetworking() {}

    /**
     * Called from {@link com.ahmad.skinchangebd.SkinChangerBD#onInitialize()}.
     * Registers all server-side packet receivers.
     */
    public static void registerServerPackets() {
        // C→S: player announces skin selection (hash + model)
        ServerPlayNetworking.registerGlobalReceiver(
                SkinSelectionPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() ->
                        SkinSyncServer.getInstance().onPlayerSkinSelection(
                            player, payload.skinHash(), payload.modelType()
                        )
                    );
                }
        );

        // C→S: player announces cape selection (hash)
        ServerPlayNetworking.registerGlobalReceiver(
                CapeSelectionPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() ->
                        SkinSyncServer.getInstance().onPlayerCapeSelection(
                            player, payload.capeHash()
                        )
                    );
                }
        );

        // C→S: player uploads full skin PNG bytes
        ServerPlayNetworking.registerGlobalReceiver(
                SkinDataPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    byte[] data = payload.data();
                    context.server().execute(() -> {
                        TextureValidator.ValidationResult vr = TextureValidator.validateSkinBytes(data);
                        if (vr.valid()) {
                            SkinSyncServer.getInstance().onSkinDataReceived(player, data);
                        } else {
                            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Rejected invalid skin from {}: {}",
                                    player.getNameForScoreboard(), vr.error());
                        }
                    });
                }
        );

        // C→S: player uploads full cape PNG bytes
        ServerPlayNetworking.registerGlobalReceiver(
                CapeDataPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    byte[] data = payload.data();
                    context.server().execute(() -> {
                        TextureValidator.ValidationResult vr = TextureValidator.validateCapeBytes(data);
                        if (vr.valid()) {
                            SkinSyncServer.getInstance().onCapeDataReceived(player, data);
                        } else {
                            SkinChangerBD.LOGGER.warn("[SkinChangerBD] Rejected invalid cape from {}: {}",
                                    player.getNameForScoreboard(), vr.error());
                        }
                    });
                }
        );

        // C→S: client requests a full sync (e.g. just joined)
        ServerPlayNetworking.registerGlobalReceiver(
                RequestSyncPayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() ->
                        SkinSyncServer.getInstance().sendAllPlayerInfoTo(player)
                    );
                }
        );

        SkinChangerBD.LOGGER.debug("[SkinChangerBD] Server-side network handlers registered");
    }

    // ── Payload definitions ───────────────────────────────────────────────────

    public record SkinSelectionPayload(String skinHash, String modelType) implements CustomPayload {
        public static final CustomPayload.Id<SkinSelectionPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.C2S_SKIN_SELECTION);
        public static final PacketCodec<PacketByteBuf, SkinSelectionPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> { buf.writeString(val.skinHash); buf.writeString(val.modelType); },
                    buf -> new SkinSelectionPayload(buf.readString(64), buf.readString(16))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CapeSelectionPayload(String capeHash) implements CustomPayload {
        public static final CustomPayload.Id<CapeSelectionPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.C2S_CAPE_SELECTION);
        public static final PacketCodec<PacketByteBuf, CapeSelectionPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> buf.writeString(val.capeHash),
                    buf -> new CapeSelectionPayload(buf.readString(64))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SkinDataPayload(byte[] data) implements CustomPayload {
        public static final CustomPayload.Id<SkinDataPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.C2S_SKIN_DATA);
        public static final PacketCodec<PacketByteBuf, SkinDataPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> buf.writeByteArray(val.data),
                    buf -> new SkinDataPayload(buf.readByteArray((int) TextureValidator.MAX_SKIN_BYTES))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CapeDataPayload(byte[] data) implements CustomPayload {
        public static final CustomPayload.Id<CapeDataPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.C2S_CAPE_DATA);
        public static final PacketCodec<PacketByteBuf, CapeDataPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> buf.writeByteArray(val.data),
                    buf -> new CapeDataPayload(buf.readByteArray((int) TextureValidator.MAX_CAPE_BYTES))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RequestSyncPayload() implements CustomPayload {
        public static final CustomPayload.Id<RequestSyncPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.C2S_REQUEST_SYNC);
        public static final PacketCodec<PacketByteBuf, RequestSyncPayload> CODEC =
                PacketCodec.of((val, buf) -> {}, buf -> new RequestSyncPayload());
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    // ── S→C Payloads (registered on client, defined here for sharing) ─────────

    public record PlayerSkinInfoPayload(String playerName, String skinHash, String modelType) implements CustomPayload {
        public static final CustomPayload.Id<PlayerSkinInfoPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_PLAYER_SKIN_INFO);
        public static final PacketCodec<PacketByteBuf, PlayerSkinInfoPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> { buf.writeString(val.playerName); buf.writeString(val.skinHash); buf.writeString(val.modelType); },
                    buf -> new PlayerSkinInfoPayload(buf.readString(32), buf.readString(64), buf.readString(16))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PlayerCapeInfoPayload(String playerName, String capeHash) implements CustomPayload {
        public static final CustomPayload.Id<PlayerCapeInfoPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_PLAYER_CAPE_INFO);
        public static final PacketCodec<PacketByteBuf, PlayerCapeInfoPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> { buf.writeString(val.playerName); buf.writeString(val.capeHash); },
                    buf -> new PlayerCapeInfoPayload(buf.readString(32), buf.readString(64))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RequestSkinUploadPayload(String expectedHash) implements CustomPayload {
        public static final CustomPayload.Id<RequestSkinUploadPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_REQUEST_SKIN);
        public static final PacketCodec<PacketByteBuf, RequestSkinUploadPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> buf.writeString(val.expectedHash),
                    buf -> new RequestSkinUploadPayload(buf.readString(64))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RequestCapeUploadPayload(String expectedHash) implements CustomPayload {
        public static final CustomPayload.Id<RequestCapeUploadPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_REQUEST_CAPE);
        public static final PacketCodec<PacketByteBuf, RequestCapeUploadPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> buf.writeString(val.expectedHash),
                    buf -> new RequestCapeUploadPayload(buf.readString(64))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ServerSkinDataPayload(String playerName, String hash, boolean isSkin, byte[] data) implements CustomPayload {
        public static final CustomPayload.Id<ServerSkinDataPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_SKIN_DATA);
        public static final PacketCodec<PacketByteBuf, ServerSkinDataPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> { buf.writeString(val.playerName); buf.writeString(val.hash); buf.writeBoolean(val.isSkin); buf.writeByteArray(val.data); },
                    buf -> new ServerSkinDataPayload(buf.readString(32), buf.readString(64), buf.readBoolean(), buf.readByteArray((int) TextureValidator.MAX_SKIN_BYTES))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ServerCapeDataPayload(String playerName, String hash, byte[] data) implements CustomPayload {
        public static final CustomPayload.Id<ServerCapeDataPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_CAPE_DATA);
        public static final PacketCodec<PacketByteBuf, ServerCapeDataPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> { buf.writeString(val.playerName); buf.writeString(val.hash); buf.writeByteArray(val.data); },
                    buf -> new ServerCapeDataPayload(buf.readString(32), buf.readString(64), buf.readByteArray((int) TextureValidator.MAX_CAPE_BYTES))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PlayerResetSkinPayload(String playerName) implements CustomPayload {
        public static final CustomPayload.Id<PlayerResetSkinPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_PLAYER_RESET_SKIN);
        public static final PacketCodec<PacketByteBuf, PlayerResetSkinPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> buf.writeString(val.playerName),
                    buf -> new PlayerResetSkinPayload(buf.readString(32))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record PlayerResetCapePayload(String playerName) implements CustomPayload {
        public static final CustomPayload.Id<PlayerResetCapePayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_PLAYER_RESET_CAPE);
        public static final PacketCodec<PacketByteBuf, PlayerResetCapePayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> buf.writeString(val.playerName),
                    buf -> new PlayerResetCapePayload(buf.readString(32))
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ServerSupportPayload(boolean supported) implements CustomPayload {
        public static final CustomPayload.Id<ServerSupportPayload> ID =
                new CustomPayload.Id<>(NetworkPackets.S2C_SERVER_SUPPORT);
        public static final PacketCodec<PacketByteBuf, ServerSupportPayload> CODEC =
                PacketCodec.of(
                    (val, buf) -> buf.writeBoolean(val.supported),
                    buf -> new ServerSupportPayload(buf.readBoolean())
                );
        @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
    }
}
