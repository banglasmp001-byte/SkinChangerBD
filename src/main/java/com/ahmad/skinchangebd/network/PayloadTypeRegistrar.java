package com.ahmad.skinchangebd.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers all custom CustomPayload types with Fabric's networking layer.
 * Must be called during mod initialization (both client and server).
 *
 * In Fabric API 0.100+ (Minecraft 1.21+), custom packets must be registered
 * via PayloadTypeRegistry before use.
 *
 * Created by Ahmad
 */
public final class PayloadTypeRegistrar {

    private PayloadTypeRegistrar() {}

    /**
     * Registers all C→S (client to server) and S→C (server to client) payload types.
     * Call once from the common (main) entrypoint.
     */
    public static void registerAll() {
        // C → S
        PayloadTypeRegistry.playC2S().register(
                SkinChangerNetworking.SkinSelectionPayload.ID,
                SkinChangerNetworking.SkinSelectionPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SkinChangerNetworking.CapeSelectionPayload.ID,
                SkinChangerNetworking.CapeSelectionPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SkinChangerNetworking.SkinDataPayload.ID,
                SkinChangerNetworking.SkinDataPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SkinChangerNetworking.CapeDataPayload.ID,
                SkinChangerNetworking.CapeDataPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SkinChangerNetworking.RequestSyncPayload.ID,
                SkinChangerNetworking.RequestSyncPayload.CODEC);

        // S → C
        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.PlayerSkinInfoPayload.ID,
                SkinChangerNetworking.PlayerSkinInfoPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.PlayerCapeInfoPayload.ID,
                SkinChangerNetworking.PlayerCapeInfoPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.RequestSkinUploadPayload.ID,
                SkinChangerNetworking.RequestSkinUploadPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.RequestCapeUploadPayload.ID,
                SkinChangerNetworking.RequestCapeUploadPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.ServerSkinDataPayload.ID,
                SkinChangerNetworking.ServerSkinDataPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.ServerCapeDataPayload.ID,
                SkinChangerNetworking.ServerCapeDataPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.PlayerResetSkinPayload.ID,
                SkinChangerNetworking.PlayerResetSkinPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.PlayerResetCapePayload.ID,
                SkinChangerNetworking.PlayerResetCapePayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SkinChangerNetworking.ServerSupportPayload.ID,
                SkinChangerNetworking.ServerSupportPayload.CODEC);
    }
}
