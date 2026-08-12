package com.ahmad.skinchangebd.network;

import com.ahmad.skinchangebd.SkinChangerBD;
import net.minecraft.util.Identifier;

/**
 * All custom network packet identifiers for SkinChangerBD.
 * Packets flow between client ↔ server for skin/cape synchronization.
 *
 * Protocol:
 *   C→S  SKIN_SELECTION  : player sends their current skin hash + model type
 *   C→S  CAPE_SELECTION  : player sends their current cape hash
 *   C→S  SKIN_DATA       : player sends full PNG bytes (only when server doesn't have it)
 *   C→S  CAPE_DATA       : player sends full cape PNG bytes
 *   S→C  PLAYER_SKIN_INFO: server broadcasts another player's skin hash + model
 *   S→C  PLAYER_CAPE_INFO: server broadcasts another player's cape hash
 *   S→C  REQUEST_SKIN    : server asks client to upload skin PNG (hash not cached)
 *   S→C  REQUEST_CAPE    : server asks client to upload cape PNG
 *   S→C  SKIN_DATA       : server forwards PNG to client that needs it
 *   S→C  CAPE_DATA       : server forwards cape PNG
 *
 * Created by Ahmad
 */
public final class NetworkPackets {

    // Client → Server
    public static final Identifier C2S_SKIN_SELECTION = id("c2s_skin_selection");
    public static final Identifier C2S_CAPE_SELECTION = id("c2s_cape_selection");
    public static final Identifier C2S_SKIN_DATA      = id("c2s_skin_data");
    public static final Identifier C2S_CAPE_DATA      = id("c2s_cape_data");
    public static final Identifier C2S_REQUEST_SYNC   = id("c2s_request_sync");

    // Server → Client
    public static final Identifier S2C_PLAYER_SKIN_INFO = id("s2c_player_skin_info");
    public static final Identifier S2C_PLAYER_CAPE_INFO = id("s2c_player_cape_info");
    public static final Identifier S2C_REQUEST_SKIN     = id("s2c_request_skin");
    public static final Identifier S2C_REQUEST_CAPE     = id("s2c_request_cape");
    public static final Identifier S2C_SKIN_DATA        = id("s2c_skin_data");
    public static final Identifier S2C_CAPE_DATA        = id("s2c_cape_data");
    public static final Identifier S2C_PLAYER_RESET_SKIN = id("s2c_player_reset_skin");
    public static final Identifier S2C_PLAYER_RESET_CAPE = id("s2c_player_reset_cape");
    public static final Identifier S2C_SERVER_SUPPORT   = id("s2c_server_support");

    private NetworkPackets() {}

    private static Identifier id(String path) {
        return Identifier.of(SkinChangerBD.MOD_ID, path);
    }
}
