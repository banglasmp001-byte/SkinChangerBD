package com.ahmad.skinchangebd.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers SkinChangerBD with Mod Menu so users get a "Config" button
 * in the Mods list.
 *
 * This class is declared as an entrypoint in fabric.mod.json under "modmenu".
 * If Mod Menu is not installed, this entrypoint is simply never invoked —
 * the rest of the mod continues normally.
 *
 * Created by Ahmad
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Return a factory that creates a new SkinChangerScreen each time
        return parent -> new SkinChangerScreen(parent);
    }
}
