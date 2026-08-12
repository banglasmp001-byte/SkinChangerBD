package com.ahmad.skinchangebd.client.keybind;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.gui.SkinChangerScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers and handles the SkinChangerBD keybind (default: K).
 * Players can rebind it in Options → Controls.
 *
 * Created by Ahmad
 */
public final class KeybindManager {

    public static final String CATEGORY = "key.category.skinchangebd";
    public static final String KEY_OPEN_GUI = "key.skinchangebd.open_gui";

    private static KeyBinding openGuiKey;

    private KeybindManager() {}

    public static void register() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_OPEN_GUI,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));
        SkinChangerBD.LOGGER.debug("[SkinChangerBD] Keybind registered: {}", KEY_OPEN_GUI);
    }

    /**
     * Called every client tick to check if the keybind was pressed.
     */
    public static void handleTick(MinecraftClient client) {
        if (openGuiKey == null) return;
        while (openGuiKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new SkinChangerScreen(null));
            }
        }
    }

    public static KeyBinding getOpenGuiKey() {
        return openGuiKey;
    }
}
