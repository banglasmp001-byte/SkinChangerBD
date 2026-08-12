package com.ahmad.skinchangebd.gui;

import com.ahmad.skinchangebd.config.ModConfig;
import com.ahmad.skinchangebd.network.ClientNetworkHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * The "Sync" tab — shows multiplayer sync status and lets the player toggle sync on/off.
 *
 * Created by Ahmad
 */
public class SyncTabScreen extends AbstractTabScreen {

    private ButtonWidget btnToggleSync;
    private ButtonWidget btnRequestSync;

    public SyncTabScreen(Screen parent, int x, int y, int width, int height) {
        super(parent, x, y, width, height);
    }

    @Override
    protected void initWidgets() {
        int cx = x + width / 2;
        int startY = y + 40;

        btnToggleSync = addButton(cx - 80, startY, 160, 20,
                getSyncToggleText(), b -> toggleSync());

        btnRequestSync = addButton(cx - 80, startY + 28, 160, 20,
                Text.translatable("skinchangebd.button.request_sync"), b -> requestSync());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        int cx = x + width / 2;
        int lineH = client.textRenderer.fontHeight + 4;
        int textY = y + 8;

        // Section heading
        context.drawCenteredTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.tab.sync"), cx, textY, 0xFFFFFF);
        textY += lineH + 4;

        // Server support status
        boolean serverOk = ClientNetworkHandler.isServerSyncAvailable();
        Text serverStatus = serverOk
                ? Text.translatable("skinchangebd.sync.server_supported").withColor(0x55FF55)
                : Text.translatable("skinchangebd.sync.server_not_supported").withColor(0xFF5555);
        context.drawCenteredTextWithShadow(client.textRenderer, serverStatus, cx, textY, 0xFFFFFF);
        textY += lineH;

        // Explanation
        if (!serverOk) {
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.translatable("skinchangebd.sync.install_hint"), cx, textY, 0x888888);
            textY += lineH;
        }

        textY += 8;

        // Sync enabled status
        boolean syncOn = ModConfig.getInstance().isMultiplayerSync();
        context.drawCenteredTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.sync.enabled").append(": ")
                    .append(syncOn
                        ? Text.translatable("skinchangebd.option.on").withColor(0x55FF55)
                        : Text.translatable("skinchangebd.option.off").withColor(0xFF5555)),
                cx, textY, 0xFFFFFF);

        textY += lineH * 2;

        // Info box
        int boxX = x + 20;
        int boxW = width - 40;
        context.fill(boxX, textY, boxX + boxW, textY + lineH * 5 + 8, 0x33FFFFFF);
        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.sync.how_it_works"), boxX + 4, textY + 4, 0xCCCCCC);
        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.sync.how_step1"), boxX + 4, textY + 4 + lineH, 0xAAAAAA);
        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.sync.how_step2"), boxX + 4, textY + 4 + lineH * 2, 0xAAAAAA);
        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.sync.how_step3"), boxX + 4, textY + 4 + lineH * 3, 0xAAAAAA);
        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.sync.how_step4"), boxX + 4, textY + 4 + lineH * 4, 0xAAAAAA);

        // Update toggle button label dynamically
        if (btnToggleSync != null) btnToggleSync.setMessage(getSyncToggleText());
        btnRequestSync.active = serverOk && syncOn;

        for (ButtonWidget btn : buttons) {
            btn.render(context, mouseX, mouseY, delta);
        }
    }

    private void toggleSync() {
        ModConfig cfg = ModConfig.getInstance();
        cfg.setMultiplayerSync(!cfg.isMultiplayerSync());
        cfg.save();
    }

    private void requestSync() {
        if (ClientNetworkHandler.isServerSyncAvailable()) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new com.ahmad.skinchangebd.network.SkinChangerNetworking.RequestSyncPayload());
        }
    }

    private Text getSyncToggleText() {
        boolean on = ModConfig.getInstance().isMultiplayerSync();
        return Text.translatable("skinchangebd.button.toggle_sync").append(": ")
               .append(on ? Text.literal("ON").withColor(0x55FF55)
                          : Text.literal("OFF").withColor(0xFF5555));
    }
}
