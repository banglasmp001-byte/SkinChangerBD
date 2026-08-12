package com.ahmad.skinchangebd.gui;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.cape.CapeEntry;
import com.ahmad.skinchangebd.cape.CapeManager;
import com.ahmad.skinchangebd.network.ClientNetworkHandler;
import com.ahmad.skinchangebd.render.SkinTextureManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The "Cape" tab inside SkinChangerBD config screen.
 * Shows: cape list, preview, import, apply, delete, refresh, reset.
 *
 * Created by Ahmad
 */
public class CapeTabScreen extends AbstractTabScreen {

    private List<CapeEntry> capes = List.of();
    private int selectedIndex = -1;
    private int scrollOffset  = 0;

    private static final int LIST_ITEM_H  = 18;
    private static final int LIST_VISIBLE = 8;
    private static final int LIST_W       = 160;
    private static final int PREVIEW_W    = 80;
    private static final int PREVIEW_H    = 60;

    private String statusMsg  = "";
    private long   statusTime = 0;

    private ButtonWidget btnApply, btnDelete, btnReset, btnRefresh, btnImport;

    public CapeTabScreen(Screen parent, int x, int y, int width, int height) {
        super(parent, x, y, width, height);
    }

    @Override
    public void init(net.minecraft.client.MinecraftClient client, int screenW, int screenH) {
        super.init(client, screenW, screenH);
        refreshList();
    }

    @Override
    protected void initWidgets() {
        int listX = x + 4;
        int listY = y + 4;
        int btnY  = listY + LIST_ITEM_H * LIST_VISIBLE + 6;
        int btnW  = 72;
        int btnH  = 18;

        btnApply   = addButton(listX,                  btnY, btnW, btnH,
                Text.translatable("skinchangebd.button.apply"),   b -> applySelected());
        btnDelete  = addButton(listX + btnW + 4,       btnY, btnW, btnH,
                Text.translatable("skinchangebd.button.delete"),  b -> deleteSelected());
        btnReset   = addButton(listX + (btnW + 4) * 2, btnY, btnW, btnH,
                Text.translatable("skinchangebd.button.reset"),   b -> resetCape());

        int btn2Y = btnY + btnH + 4;
        btnRefresh = addButton(listX,                  btn2Y, btnW, btnH,
                Text.translatable("skinchangebd.button.refresh"), b -> refreshList());
        btnImport  = addButton(listX + btnW + 4,       btn2Y, btnW, btnH,
                Text.translatable("skinchangebd.button.import"),  b -> openImportDialog());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        int listX = x + 4;
        int listY = y + 4;

        context.drawTextWithShadow(client.textRenderer,
                Text.translatable("skinchangebd.label.capes"), listX, listY - 12, 0xAAAAAA);

        // List background
        context.fill(listX - 1, listY - 1, listX + LIST_W + 1, listY + LIST_ITEM_H * LIST_VISIBLE + 1, 0xFF333333);

        for (int i = 0; i < LIST_VISIBLE && (i + scrollOffset) < capes.size(); i++) {
            int idx   = i + scrollOffset;
            CapeEntry e = capes.get(idx);
            int itemY = listY + i * LIST_ITEM_H;
            boolean sel = (idx == selectedIndex);
            int bg = sel ? 0xFF5555AA : (i % 2 == 0 ? 0xFF2A2A2A : 0xFF252525);
            context.fill(listX, itemY, listX + LIST_W, itemY + LIST_ITEM_H, bg);

            String name = e.name();
            if (client.textRenderer.getWidth(name) > LIST_W - 4) {
                name = client.textRenderer.trimToWidth(name, LIST_W - 4);
            }
            context.drawTextWithShadow(client.textRenderer, name, listX + 4, itemY + 5,
                    sel ? 0xFFFFFF : 0xCCCCCC);
        }

        // Empty list hint
        if (capes.isEmpty()) {
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.translatable("skinchangebd.label.no_capes"),
                    listX + LIST_W / 2, listY + LIST_ITEM_H * 3, 0x888888);
        }

        // Preview panel
        int prevX = x + LIST_W + 16;
        int prevY = y + 4;
        context.fill(prevX - 1, prevY - 1, prevX + PREVIEW_W + 1, prevY + PREVIEW_H + 1, 0xFF444444);
        context.fill(prevX, prevY, prevX + PREVIEW_W, prevY + PREVIEW_H, 0xFF1A1A1A);

        if (selectedIndex >= 0 && selectedIndex < capes.size()) {
            renderCapePreview(context, prevX, prevY, capes.get(selectedIndex));
        } else {
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.translatable("skinchangebd.label.no_cape"),
                    prevX + PREVIEW_W / 2, prevY + PREVIEW_H / 2 - 4, 0x888888);
        }

        // Active cape info
        CapeEntry active = CapeManager.getInstance().getActiveCape();
        if (active != null) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.translatable("skinchangebd.label.active").append(": " + active.name()),
                    x + 4, y + height - 32, 0x55FF55);
        }

        // Status
        if (!statusMsg.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal(statusMsg), x + width / 2, y + height - 14, 0xFFFF55);
        }

        for (ButtonWidget btn : buttons) {
            btn.render(context, mouseX, mouseY, delta);
        }
    }

    private void renderCapePreview(DrawContext context, int px, int py, CapeEntry entry) {
        Identifier texId = SkinTextureManager.getInstance().getOwnCapeIdentifier();

        if (texId != null && entry.equals(CapeManager.getInstance().getActiveCape())) {
            // Cape UV: full texture is typically 64x32
            // Render the cape body area (u=1, v=1, w=10, h=16 of 64x32)
            context.drawTexture(texId,
                    px + PREVIEW_W / 2 - 20, py + 8,
                    40, 44,
                    1, 1, 10, 16, 64, 32);
        } else {
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal(entry.name()), px + PREVIEW_W / 2, py + 22, 0xFFFFFF);
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.translatable("skinchangebd.label.apply_preview"),
                    px + PREVIEW_W / 2, py + 36, 0x888888);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void applySelected() {
        if (selectedIndex < 0 || selectedIndex >= capes.size()) return;
        CapeEntry entry = capes.get(selectedIndex);
        CapeManager.getInstance().selectCape(entry);
        SkinTextureManager.getInstance().loadAndApplyCape(entry.path());
        if (ClientNetworkHandler.isServerSyncAvailable()) {
            ClientNetworkHandler.announceCurrentSelections();
        }
        setStatus("skinchangebd.status.cape_applied");
    }

    private void deleteSelected() {
        if (selectedIndex < 0 || selectedIndex >= capes.size()) return;
        CapeEntry entry = capes.get(selectedIndex);
        if (CapeManager.getInstance().deleteCape(entry)) {
            refreshList();
            setStatus("skinchangebd.status.cape_deleted");
        }
    }

    private void resetCape() {
        CapeManager.getInstance().resetCape();
        SkinTextureManager.getInstance().clearOwnCape();
        if (ClientNetworkHandler.isServerSyncAvailable()) {
            ClientNetworkHandler.announceCurrentSelections();
        }
        setStatus("skinchangebd.status.cape_reset");
    }

    private void refreshList() {
        Thread t = new Thread(() -> {
            CapeManager.getInstance().refresh();
            if (client != null) {
                client.execute(() -> {
                    capes = CapeManager.getInstance().getEntries();
                    selectedIndex = -1;
                    scrollOffset  = 0;
                    CapeEntry active = CapeManager.getInstance().getActiveCape();
                    if (active != null) {
                        for (int i = 0; i < capes.size(); i++) {
                            if (capes.get(i).name().equals(active.name())) {
                                selectedIndex = i;
                                break;
                            }
                        }
                    }
                });
            }
        }, "SkinChangerBD-CapeRefresh");
        t.setDaemon(true);
        t.start();
        capes = CapeManager.getInstance().getEntries();
    }

    private void openImportDialog() {
        if (client != null) {
            client.setScreen(new ImportFileScreen(parent, false, importedPath -> {
                if (importedPath != null) {
                    CapeEntry imported = CapeManager.getInstance().importCape(importedPath);
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
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        int listX = x + 4;
        int listY = y + 4;

        if (mouseX >= listX && mouseX < listX + LIST_W &&
            mouseY >= listY && mouseY < listY + LIST_ITEM_H * LIST_VISIBLE) {
            int clicked = (int)(mouseY - listY) / LIST_ITEM_H + scrollOffset;
            if (clicked >= 0 && clicked < capes.size()) {
                selectedIndex = clicked;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setStatus(String key) {
        try {
            statusMsg = net.minecraft.text.Text.translatable(key).getString();
        } catch (Exception e) {
            statusMsg = key;
        }
        statusTime = System.currentTimeMillis();
    }
}
