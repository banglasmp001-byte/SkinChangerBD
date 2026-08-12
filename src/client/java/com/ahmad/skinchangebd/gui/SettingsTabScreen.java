package com.ahmad.skinchangebd.gui;

import com.ahmad.skinchangebd.config.ModConfig;
import com.ahmad.skinchangebd.client.keybind.KeybindManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * The "Settings" tab — general toggles and information.
 *
 * Created by Ahmad
 */
public class SettingsTabScreen extends AbstractTabScreen {

    private ButtonWidget btnToggleMod;
    private ButtonWidget btnToggleSkin;
    private ButtonWidget btnToggleCape;
    private ButtonWidget btnTogglePreview;
    private ButtonWidget btnOpenFolder;

    public SettingsTabScreen(Screen parent, int x, int y, int width, int height) {
        super(parent, x, y, width, height);
    }

    @Override
    protected void initWidgets() {
        int bx   = x + 20;
        int bw   = 200;
        int bh   = 18;
        int gap  = 24;
        int startY = y + 20;

        btnToggleMod     = addButton(bx, startY,          bw, bh, getModText(),     b -> { ModConfig.getInstance().setEnabled(!ModConfig.getInstance().isEnabled()); save(); });
        btnToggleSkin    = addButton(bx, startY + gap,     bw, bh, getSkinText(),    b -> { ModConfig.getInstance().setSkinEnabled(!ModConfig.getInstance().isSkinEnabled()); save(); });
        btnToggleCape    = addButton(bx, startY + gap * 2, bw, bh, getCapeText(),    b -> { ModConfig.getInstance().setCapeEnabled(!ModConfig.getInstance().isCapeEnabled()); save(); });
        btnTogglePreview = addButton(bx, startY + gap * 3, bw, bh, getPreviewText(), b -> { ModConfig.getInstance().setPreviewEnabled(!ModConfig.getInstance().isPreviewEnabled()); save(); });

        btnOpenFolder = addButton(bx, startY + gap * 5, bw, bh,
                Text.translatable("skinchangebd.button.open_folder"), b -> openFolder());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Update button labels dynamically
        if (btnToggleMod     != null) btnToggleMod.setMessage(getModText());
        if (btnToggleSkin    != null) btnToggleSkin.setMessage(getSkinText());
        if (btnToggleCape    != null) btnToggleCape.setMessage(getCapeText());
        if (btnTogglePreview != null) btnTogglePreview.setMessage(getPreviewText());

        int cx = x + width / 2;
        context.drawCenteredTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.tab.settings"), cx, y + 6, 0xFFFFFF);

        // Keybind info
        int infoY = y + 20 + 24 * 4 + 10;
        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.settings.keybind_info").append(": "),
                x + 20, infoY, 0xAAAAAA);

        String keyName = KeybindManager.getOpenGuiKey() != null
                ? KeybindManager.getOpenGuiKey().getBoundKeyLocalizedText().getString()
                : "K";
        context.drawTextWithShadow(client.textRenderer,
                Text.literal(keyName), x + 20 + client.textRenderer.getWidth(
                        Text.translatable("skinchangebd.settings.keybind_info").getString() + ": "),
                infoY, 0xFFFF55);

        infoY += 12;
        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.settings.rebind_hint"),
                x + 20, infoY, 0x777777);

        // Version credit
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("SkinChangerBD — Created by Ahmad"),
                x + 20, y + height - 20, 0x555555);

        for (ButtonWidget btn : buttons) {
            btn.render(context, mouseX, mouseY, delta);
        }
    }

    private void save() {
        ModConfig.getInstance().save();
    }

    private void openFolder() {
        try {
            java.awt.Desktop.getDesktop().open(
                    com.ahmad.skinchangebd.SkinChangerBD.SKIN_CHANGER_DIR.toFile());
        } catch (Exception e) {
            // Desktop API not available on all platforms (e.g. Linux headless, Android)
            com.ahmad.skinchangebd.SkinChangerBD.LOGGER.warn(
                    "[SkinChangerBD] Cannot open folder via Desktop API: {}", e.getMessage());
        }
    }

    private Text getModText() {
        boolean on = ModConfig.getInstance().isEnabled();
        return Text.translatable("skinchangebd.settings.mod_enabled").append(": ")
               .append(on ? Text.literal("ON").withColor(0x55FF55) : Text.literal("OFF").withColor(0xFF5555));
    }

    private Text getSkinText() {
        boolean on = ModConfig.getInstance().isSkinEnabled();
        return Text.translatable("skinchangebd.settings.skin_enabled").append(": ")
               .append(on ? Text.literal("ON").withColor(0x55FF55) : Text.literal("OFF").withColor(0xFF5555));
    }

    private Text getCapeText() {
        boolean on = ModConfig.getInstance().isCapeEnabled();
        return Text.translatable("skinchangebd.settings.cape_enabled").append(": ")
               .append(on ? Text.literal("ON").withColor(0x55FF55) : Text.literal("OFF").withColor(0xFF5555));
    }

    private Text getPreviewText() {
        boolean on = ModConfig.getInstance().isPreviewEnabled();
        return Text.translatable("skinchangebd.settings.preview_enabled").append(": ")
               .append(on ? Text.literal("ON").withColor(0x55FF55) : Text.literal("OFF").withColor(0xFF5555));
    }
}
