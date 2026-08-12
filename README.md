# SkinChangerBD

> **Created by Ahmad**

A production-ready **Fabric mod** for Minecraft Java Edition that lets players manage and
apply custom Skins and Capes — with full **multiplayer synchronization** so other
SkinChangerBD users on the same server see your custom skin and cape in real time.

---

## Features

### Skin System
- Import PNG skins (64×64 or 64×32)
- Classic (Steve) and Slim (Alex) model support
- Apply, preview, delete, reset skins
- Skin library auto-scanned from the `SkinChangerBD/skin/` folder
- Refresh list without restarting Minecraft
- Persistent selection — your skin is remembered across restarts
- Full validation — corrupt or invalid PNGs never crash the game

### Cape System
- Import PNG capes (64×32 or 46×22)
- Apply, preview, delete, reset capes
- Cape library auto-scanned from `SkinChangerBD/cape/`
- Same safety and persistence guarantees as the skin system

### Multiplayer Synchronization
- When both you and another player have SkinChangerBD installed (client **and** server),
  you see each other's custom skins and capes
- Efficient protocol: only the SHA-256 hash is sent first; the PNG is uploaded only once
  and cached — never re-transmitted to clients that already have it
- Graceful degradation: if the server does not have SkinChangerBD, local skin/cape
  management still works perfectly and the game never crashes
- Per-session sync: updates broadcast on join, skin change, cape change, and disconnect

### GUI
- Opened with **K** (configurable in Options → Controls → SkinChangerBD)
- Or via **Mods → SkinChangerBD → Config** (requires Mod Menu)
- Tabs: **Skin | Cape | Sync | Settings**
- Skin and Cape tabs show a scrollable library list + preview panel
- Sync tab shows live server support status and sync toggle
- Settings tab has per-feature toggles and folder shortcut

### Import
- Type or paste the full path to any PNG file in the import dialog
- Or drop PNG files directly into the skin/cape folders (then press Refresh)
- On desktop, the Settings tab has an "Open Folder" button

### Mod Menu Integration
- Fully integrated with the official [Mod Menu](https://modrinth.com/mod/modmenu) mod
- Config button appears in the Mods list
- No crash if Mod Menu is not installed

---

## Folder Structure

```
.minecraft/
└── SkinChangerBD/
    ├── skin/          ← place or import your skin PNGs here
    ├── cape/          ← place or import your cape PNGs here
    └── cache/
        ├── skin/      ← cached remote player skins (multiplayer)
        └── cape/      ← cached remote player capes (multiplayer)
```

Created automatically on first launch. You never need to make these folders manually.

---

## Supported Minecraft Versions

| Minecraft | Fabric API         | Status  |
|-----------|--------------------|---------|
| 1.21.1    | 0.107.0+1.21.1     | ✅ Supported |
| 1.21.2    | 0.107.1+1.21.2     | ✅ Supported |
| 1.21.3    | 0.107.3+1.21.3     | ✅ Supported |
| 1.21.4    | 0.110.5+1.21.4     | ✅ Supported |
| 1.21.5    | 0.119.0+1.21.5     | ✅ Supported |

Each version produces its own JAR: `SkinChangerBD-1.21.x.jar`

---

## Requirements

| Requirement | Version  |
|-------------|----------|
| Java        | 21+      |
| Fabric Loader | ≥ 0.15.11 |
| Fabric API  | Required |
| Mod Menu    | Optional (recommended) |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your Minecraft version
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (required)
3. Optionally install [Mod Menu](https://modrinth.com/mod/modmenu)
4. Download the correct `SkinChangerBD-1.21.x.jar` for your Minecraft version
5. Place it in `.minecraft/mods/`
6. Launch Minecraft

**For multiplayer sync:** install the same JAR on the server too (it runs on both sides).

---

## Mod Menu Configuration

With Mod Menu installed:

```
Minecraft → Mods → SkinChangerBD → Config
```

This opens the full SkinChangerBD configuration screen with all skin, cape, sync,
and settings tabs.

---

## Multiplayer Synchronization

### How it works

```
Player A applies custom skin → sends hash to server
                             → server caches PNG (uploaded once)
                             → broadcasts hash to other players
Player B receives hash       → checks local cache
                             → if not cached: server sends PNG
                             → renders Player A's custom skin
```

### Server-side setup

Install the same `SkinChangerBD-1.21.x.jar` into your server's `mods/` folder.

If the server does **not** have SkinChangerBD:
- Your local skin/cape management works normally
- Multiplayer sync is unavailable (shown in the Sync tab)
- No crashes, no errors

### PojavLauncher / Android

SkinChangerBD works with PojavLauncher. The game directory is used for all paths,
so `SkinChangerBD/skin/` and `SkinChangerBD/cape/` appear in the correct location
inside PojavLauncher's `.minecraft/` equivalent.

Import skins using the file-path dialog (paste a path from your file manager).

---

## Keybind

| Action         | Default Key | Category      |
|----------------|-------------|---------------|
| Open SkinChangerBD | K      | SkinChangerBD |

Rebind in: **Options → Controls → SkinChangerBD**

---

## Building from Source

### Prerequisites
- Java 21 JDK
- Git

### Build steps

```bash
git clone https://github.com/ahmad/SkinChangerBD.git
cd SkinChangerBD
chmod +x gradlew
./gradlew build
```

Output JARs are in `build/libs/`.

To build for a specific Minecraft version:

```bash
./gradlew build \
  -Pminecraft_version=1.21.4 \
  -Pyarn_mappings=1.21.4+build.8 \
  -Pfabric_version=0.110.5+1.21.4 \
  -Pmodmenu_version=13.0.0+1.21.4
```

### GitHub Actions

Push to `main` or open a PR — GitHub Actions builds all supported versions automatically.
Artifacts are uploaded per-version. Tagging a release (`v1.0.0`) triggers a GitHub Release
with all JARs attached.

---

## Configuration File

Stored at: `.minecraft/config/skinchangebd.json`

```json
{
  "enabled": true,
  "skinEnabled": true,
  "capeEnabled": true,
  "multiplayerSync": true,
  "autoRefresh": false,
  "previewEnabled": true,
  "selectedSkin": "my_skin",
  "selectedCape": "my_cape",
  "skinModelType": "classic",
  "cacheMaxMb": 50
}
```

---

## Texture Validation

SkinChangerBD validates every PNG before loading:

| Check | Detail |
|-------|--------|
| PNG header | Magic bytes 0x89 PNG |
| File size | Max 256 KB per texture |
| Skin dimensions | 64×64 (modern) or 64×32 (legacy) |
| Cape dimensions | 64×32 or 46×22 |
| Path safety | No path traversal; files stay in their folders |

Invalid files are logged and skipped — Minecraft never crashes.

---

## Package Structure

```
com.ahmad.skinchangebd/
├── SkinChangerBD.java          — main entrypoint
├── config/
│   └── ModConfig.java          — JSON config persistence
├── skin/
│   ├── SkinEntry.java          — skin data record
│   └── SkinManager.java        — scan, import, delete, select
├── cape/
│   ├── CapeEntry.java          — cape data record
│   └── CapeManager.java        — scan, import, delete, select
├── network/
│   ├── NetworkPackets.java     — packet Identifiers
│   ├── PayloadTypeRegistrar.java — registers payloads with Fabric
│   ├── SkinChangerNetworking.java — payload types + server handlers
│   └── ClientNetworkHandler.java  — client-side handlers (client src)
├── server/
│   └── SkinSyncServer.java     — server sync engine
├── render/
│   └── SkinTextureManager.java — GPU texture cache (client src)
├── gui/
│   ├── SkinChangerScreen.java  — main tabbed screen
│   ├── SkinTabScreen.java      — skin management tab
│   ├── CapeTabScreen.java      — cape management tab
│   ├── SyncTabScreen.java      — sync status tab
│   ├── SettingsTabScreen.java  — settings tab
│   ├── ImportFileScreen.java   — file path import dialog
│   └── ModMenuIntegration.java — Mod Menu API integration
├── mixin/
│   └── PlayerEntityRendererMixin.java — overrides player texture lookup
└── util/
    ├── TextureValidator.java   — PNG validation + path sanitization
    └── TextureHash.java        — SHA-256 hashing
```

---

## Credits

**Created by Ahmad**

Built with:
- [Fabric Loader](https://fabricmc.net/) — MIT License
- [Fabric API](https://github.com/FabricMC/fabric) — Apache 2.0
- [Mod Menu](https://github.com/TerraformersMC/ModMenu) — MIT License

---

## License

MIT License — see [LICENSE](LICENSE)
