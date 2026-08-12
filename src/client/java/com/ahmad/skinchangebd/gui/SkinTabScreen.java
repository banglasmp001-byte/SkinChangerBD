package com.ahmad.skinchangebd.gui;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.config.ModConfig;
import com.ahmad.skinchangebd.network.ClientNetworkHandler;
import com.ahmad.skinchangebd.render.SkinTextureManager;
import com.ahmad.skinchangebd.skin.SkinEntry;
import com.ahmad.skinchangebd.skin.SkinManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.List;

/**
 * The "Skin" tab inside SkinChangerBD config screen.
 * Shows: skin list, preview, model toggle, import, apply, delete, refresh, reset.
 *
 * Created by Ahmad
 */
public class SkinTabScreen extends AbstractTabScreen {

    private List<SkinEntry> skins = List.of();
    private int selectedIndex = -1;
    private int scrollOffset  = 0;

    private static final int LIST_ITEM_H  = 18;
    private static final int LIST_VISIBLE = 8;
    private static final int LIST_W       = 160;
    private static final int PREVIEW_W    = 80;
    private static final int PREVIEW_H    = 120;

    // Status message (shown briefly after operations)
    private String statusMsg  = "";
    private long   statusTime = 0;

    private ButtonWidget btnApply, btnDelete, btnReset, btnRefresh, btnImport, btnModelToggle;

    public SkinTabScreen(Screen parent, int x, int y, int width, int height) {
        super(parent, x, y, width, height);
    }

    @Override
    public void init(net.minecraft.client.MinecraftClient client, int screenW, int screenH) {
        super.init(client, screenW, screenH);
        refreshList();
    }

    @Override
    protected void initWidgets() {
        int listX     = x + 4;
        int listY     = y + 4;
        int btnY      = listY + LIST_ITEM_H * LIST_VISIBLE + 6;
        int btnW      = 72;
        int btnH      = 18;

        btnApply = addButton(listX, btnY, btnW, btnH,
                Text.translatable("skinchangebd.button.apply"), b -> applySelected());

        btnDelete = addButton(listX + btnW + 4, btnY, btnW, btnH,
                Text.translatable("skinchangebd.button.delete"), b -> deleteSelected());

        btnReset = addButton(listX + (btnW + 4) * 2, btnY, btnW, btnH,
                Text.translatable("skinchangebd.button.reset"), b -> resetSkin());

        int btn2Y = btnY + btnH + 4;
        btnRefresh = addButton(listX, btn2Y, btnW, btnH,
                Text.translatable("skinchangebd.button.refresh"), b -> refreshList());

        btnImport = addButton(listX + btnW + 4, btn2Y, btnW, btnH,
                Text.translatable("skinchangebd.button.import"), b -> openImportDialog());

        btnModelToggle = addButton(listX + (btnW + 4) * 2, btn2Y, btnW, btnH,
                getModelToggleText(), b -> toggleModel());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        int listX = x + 4;
        int listY = y + 4;

        // Section label
        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.label.skins"),
                listX, listY - 12, 0xAAAAAA);

        // Skin list background
        context.fill(listX - 1, listY - 1, listX + LIST_W + 1, listY + LIST_ITEM_H * LIST_VISIBLE + 1, 0xFF333333);

        // Skin list items
        for (int i = 0; i < LIST_VISIBLE && (i + scrollOffset) < skins.size(); i++) {
            int idx    = i + scrollOffset;
            SkinEntry e = skins.get(idx);
            int itemY  = listY + i * LIST_ITEM_H;

            boolean sel = (idx == selectedIndex);
            int bg = sel ? 0xFF5555AA : (i % 2 == 0 ? 0xFF2A2A2A : 0xFF252525);
            context.fill(listX, itemY, listX + LIST_W, itemY + LIST_ITEM_H, bg);

            // Truncate long names
            String name = e.name();
            if (client.textRenderer.getWidth(name) > LIST_W - 4) {
                name = client.textRenderer.trimToWidth(name, LIST_W - 4);
            }
            context.drawTextWithShadow(client.textRenderer, name, listX + 4, itemY + 5, sel ? 0xFFFFFF : 0xCCCCCC);
        }

        // Preview panel on the right
        int previewX = x + LIST_W + 16;
        int previewY = y + 4;
        context.fill(previewX - 1, previewY - 1, previewX + PREVIEW_W + 1, previewY + PREVIEW_H + 1, 0xFF444444);
        context.fill(previewX, previewY, previewX + PREVIEW_W, previewY + PREVIEW_H, 0xFF1A1A1A);

        // Show skin preview if a skin is selected
        if (selectedIndex >= 0 && selectedIndex < skins.size()) {
            SkinEntry selected = skins.get(selectedIndex);
            renderSkinPreview(context, previewX, previewY, selected);
        } else {
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.translatable("skinchangebd.label.no_skin"),
                    previewX + PREVIEW_W / 2, previewY + PREVIEW_H / 2 - 4, 0x888888);
        }

        // Active skin info
        SkinEntry active = SkinManager.getInstance().getActiveSkin();
        if (active != null) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.translatable("skinchangebd.label.active").append(": " + active.name()),
                    x + 4, y + height - 32, 0x55FF55);
        }

        // Status message
        if (!statusMsg.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal(statusMsg), x + width / 2, y + height - 14, 0xFFFF55);
        }

        // Render buttons
        for (ButtonWidget btn : buttons) {
            btn.render(context, mouseX, mouseY, delta);
        }
    }

    private void renderSkinPreview(DrawContext context, int px, int py, SkinEntry entry) {
        // Attempt to get texture identifier
        Identifier texId = SkinTextureManager.getInstance().getOwnSkinIdentifier();
        if (texId == null || !entry.equals(SkinManager.getInstance().getActiveSkin())) {
            // Try loading preview for non-active skin
            texId = null; // Can't easily preview without loading
        }

        if (texId != null) {
            // Draw a simple head + body sprite from the skin texture
            // Head (uv: 8/64, 8/32, 8/64, 8/32 in 64x64)
            drawSkinHead(context, texId, px + PREVIEW_W / 2 - 16, py + 8, 32, 32);
            // Body (simplified)
            drawSkinBody(context, texId, px + PREVIEW_W / 2 - 12, py + 40, 24, 32);
        } else {
            // Show placeholder text
            String model = entry.isSlim() ? "Slim" : "Classic";
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal(entry.name()), px + PREVIEW_W / 2, py + 54, 0xFFFFFF);
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal("[" + model + "]"), px + PREVIEW_W / 2, py + 66, 0xAAAAAA);
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.translatable("skinchangebd.label.apply_preview"),
                    px + PREVIEW_W / 2, py + 82, 0x888888);
        }
    }

    private void drawSkinHead(DrawContext context, Identifier texture, int dx, int dy, int w, int h) {
        // Draw head from skin UV: face is at u=8,v=8 size 8x8 in a 64x64 texture
        context.drawTexture(texture, dx, dy, w, h, 8, 8, 8, 8, 64, 64);
    }

    private void drawSkinBody(DrawContext context, Identifier texture, int dx, int dy, int w, int h) {
        // Body front: u=20,v=20 size 8x12 in 64x64
        context.drawTexture(texture, dx, dy, w, h, 20, 20, 8, 12, 64, 64);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void applySelected() {
        if (selectedIndex < 0 || selectedIndex >= skins.size()) return;
        SkinEntry entry = skins.get(selectedIndex);
        SkinManager.getInstance().selectSkin(entry);

        // Load texture on render thread
        SkinTextureManager.getInstance().loadAndApplySkin(entry.path());

        // Announce to server if sync is on
        if (ClientNetworkHandler.isServerSyncAvailable()) {
            ClientNetworkHandler.announceCurrentSelections();
        }

        setStatus("skinchangebd.status.skin_applied");
    }

    private void deleteSelected() {
        if (selectedIndex < 0 || selectedIndex >= skins.size()) return;
        SkinEntry entry = skins.get(selectedIndex);
        if (SkinManager.getInstance().deleteSkin(entry)) {
            refreshList();
            setStatus("skinchangebd.status.skin_deleted");
        }
    }

    private void resetSkin() {
        SkinManager.getInstance().resetSkin();
        SkinTextureManager.getInstance().clearOwnSkin();
        if (ClientNetworkHandler.isServerSyncAvailable()) {
            ClientNetworkHandler.announceCurrentSelections();
        }
        setStatus("skinchangebd.status.skin_reset");
    }

    private void refreshList() {
        Thread t = new Thread(() -> {
            SkinManager.getInstance().refresh();
            // Back on main thread
            if (client != null) {
                client.execute(() -> {
                    skins = SkinManager.getInstance().getEntries();
                    selectedIndex = -1;
                    scrollOffset  = 0;
                    // Try to highlight the active skin
                    SkinEntry active = SkinManager.getInstance().getActiveSkin();
                    if (active != null) {
                        for (int i = 0; i < skins.size(); i++) {
                            if (skins.get(i).name().equals(active.name())) {
                                selectedIndex = i;
                                break;
                            }
                        }
                    }
                });
            }
        }, "SkinChangerBD-Refresh");
        t.setDaemon(true);
        t.start();

        skins = SkinManager.getInstance().getEntries();
    }

    private void openImportDialog() {
        // Use Minecraft's file dialog via try-offer, fallback to instructions
        try {
            // Platform-specific file picker is not available in vanilla Fabric
            // We guide the user to drop files in the folder
            if (client != null) {
                client.setScreen(new ImportFileScreen(parent, true, importedPath -> {
                    if (importedPath != null) {
                        String model = ModConfig.getInstance().getSkinModelType();
                        SkinEntry imported = SkinManager.getInstance().importSkin(importedPath, model);
                        if (imported != null) {
                            refreshList();
                            setStatus("Imported: " + imported.name());
                        } else {
                            setStatus("skinchangebd.status.import_failed");
                        }
                    }
                    client.setScreen(parent);
                }));
            }
        } catch (Exception e) {
            SkinChangerBD.LOGGER.error("[SkinChangerBD] Failed to open import dialog: {}", e.getMessage());
        }
    }

    private void toggleModel() {
        ModConfig cfg = ModConfig.getInstance();
        String newModel = cfg.isSlimModel() ? "classic" : "slim";
        cfg.setSkinModelType(newModel);
        cfg.save();
        if (btnModelToggle != null) btnModelToggle.setMessage(getModelToggleText());

        // If a skin is selected, update its model type in the list
        if (selectedIndex >= 0 && selectedIndex < skins.size()) {
            // Re-scan so the entry reflects the new model
            refreshList();
        }
    }

    private Text getModelToggleText() {
        boolean slim = ModConfig.getInstance().isSlimModel();
        return Text.translatable("skinchangebd.button.model").append(": ")
               .append(slim ? "Slim" : "Classic");
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        int listX = x + 4;
        int listY = y + 4;

        // Check list click
        if (mouseX >= listX && mouseX < listX + LIST_W &&
            mouseY >= listY && mouseY < listY + LIST_ITEM_H * LIST_VISIBLE) {
            int clicked = (int)(mouseY - listY) / LIST_ITEM_H + scrollOffset;
            if (clicked >= 0 && clicked < skins.size()) {
                selectedIndex = clicked;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setStatus(String key) {
        try {
            statusMsg  = net.minecraft.text.Text.translatable(key).getString();
        } catch (Exception e) {
            statusMsg = key;
        }
        statusTime = System.currentTimeMillis();
    }
}
