package com.ahmad.skinchangebd.gui;

import com.ahmad.skinchangebd.SkinChangerBD;
import com.ahmad.skinchangebd.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Main SkinChangerBD configuration screen.
 * Opened via keybind (K) or Mod Menu → Config.
 *
 * Tabs: Skin | Cape | Sync | Settings
 *
 * Created by Ahmad
 */
public class SkinChangerScreen extends Screen {

    private final Screen parent;

    // Tab indices
    private static final int TAB_SKIN     = 0;
    private static final int TAB_CAPE     = 1;
    private static final int TAB_SYNC     = 2;
    private static final int TAB_SETTINGS = 3;

    private int currentTab = TAB_SKIN;

    // Sub-screens (rendered inside the tab content area)
    private SkinTabScreen      skinTab;
    private CapeTabScreen      capeTab;
    private SyncTabScreen      syncTab;
    private SettingsTabScreen  settingsTab;

    // Tab buttons
    private ButtonWidget btnSkin;
    private ButtonWidget btnCape;
    private ButtonWidget btnSync;
    private ButtonWidget btnSettings;
    private ButtonWidget btnClose;

    // Layout constants
    private static final int HEADER_H  = 24;
    private static final int TAB_BTN_W = 72;
    private static final int TAB_BTN_H = 20;
    private static final int FOOTER_H  = 28;

    public SkinChangerScreen(Screen parent) {
        super(Text.translatable("skinchangebd.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int tabY  = HEADER_H + 2;
        int totalW = TAB_BTN_W * 4 + 6 * 3;
        int startX = (this.width - totalW) / 2;

        // Tab buttons
        btnSkin = addDrawableChild(ButtonWidget.builder(
                Text.translatable("skinchangebd.tab.skin"),
                b -> switchTab(TAB_SKIN))
                .dimensions(startX, tabY, TAB_BTN_W, TAB_BTN_H).build());

        btnCape = addDrawableChild(ButtonWidget.builder(
                Text.translatable("skinchangebd.tab.cape"),
                b -> switchTab(TAB_CAPE))
                .dimensions(startX + TAB_BTN_W + 6, tabY, TAB_BTN_W, TAB_BTN_H).build());

        btnSync = addDrawableChild(ButtonWidget.builder(
                Text.translatable("skinchangebd.tab.sync"),
                b -> switchTab(TAB_SYNC))
                .dimensions(startX + (TAB_BTN_W + 6) * 2, tabY, TAB_BTN_W, TAB_BTN_H).build());

        btnSettings = addDrawableChild(ButtonWidget.builder(
                Text.translatable("skinchangebd.tab.settings"),
                b -> switchTab(TAB_SETTINGS))
                .dimensions(startX + (TAB_BTN_W + 6) * 3, tabY, TAB_BTN_W, TAB_BTN_H).build());

        // Close button in footer
        btnClose = addDrawableChild(ButtonWidget.builder(
                Text.translatable("skinchangebd.button.close"),
                b -> close())
                .dimensions(this.width / 2 - 50, this.height - FOOTER_H + 4, 100, 20).build());

        // Content area bounds
        int contentX = 8;
        int contentY = HEADER_H + TAB_BTN_H + 6;
        int contentW = this.width - 16;
        int contentH = this.height - contentY - FOOTER_H;

        // Initialize tab sub-screens
        skinTab     = new SkinTabScreen(this, contentX, contentY, contentW, contentH);
        capeTab     = new CapeTabScreen(this, contentX, contentY, contentW, contentH);
        syncTab     = new SyncTabScreen(this, contentX, contentY, contentW, contentH);
        settingsTab = new SettingsTabScreen(this, contentX, contentY, contentW, contentH);

        skinTab.init(this.client, this.width, this.height);
        capeTab.init(this.client, this.width, this.height);
        syncTab.init(this.client, this.width, this.height);
        settingsTab.init(this.client, this.width, this.height);

        switchTab(currentTab);
    }

    private void switchTab(int tab) {
        // Deactivate all tabs first
        if (skinTab     != null) skinTab.setVisible(false);
        if (capeTab     != null) capeTab.setVisible(false);
        if (syncTab     != null) syncTab.setVisible(false);
        if (settingsTab != null) settingsTab.setVisible(false);

        currentTab = tab;

        switch (tab) {
            case TAB_SKIN     -> { if (skinTab != null)     skinTab.setVisible(true);     }
            case TAB_CAPE     -> { if (capeTab != null)     capeTab.setVisible(true);     }
            case TAB_SYNC     -> { if (syncTab != null)     syncTab.setVisible(true);     }
            case TAB_SETTINGS -> { if (settingsTab != null) settingsTab.setVisible(true); }
        }

        // Highlight active tab button
        btnSkin.active     = (tab != TAB_SKIN);
        btnCape.active     = (tab != TAB_CAPE);
        btnSync.active     = (tab != TAB_SYNC);
        btnSettings.active = (tab != TAB_SETTINGS);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Title bar
        context.fill(0, 0, this.width, HEADER_H, 0xCC000000);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("skinchangebd.screen.title"),
                this.width / 2, (HEADER_H - 8) / 2, 0xFFFFFF);

        // Footer bar
        context.fill(0, this.height - FOOTER_H, this.width, this.height, 0xCC000000);

        // Render active tab content
        switch (currentTab) {
            case TAB_SKIN     -> { if (skinTab     != null) skinTab.render(context, mouseX, mouseY, delta);     }
            case TAB_CAPE     -> { if (capeTab     != null) capeTab.render(context, mouseX, mouseY, delta);     }
            case TAB_SYNC     -> { if (syncTab     != null) syncTab.render(context, mouseX, mouseY, delta);     }
            case TAB_SETTINGS -> { if (settingsTab != null) settingsTab.render(context, mouseX, mouseY, delta); }
        }

        // Tab buttons & close button (drawn on top)
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Forward to active tab
        boolean handled = switch (currentTab) {
            case TAB_SKIN     -> skinTab     != null && skinTab.mouseClicked(mouseX, mouseY, button);
            case TAB_CAPE     -> capeTab     != null && capeTab.mouseClicked(mouseX, mouseY, button);
            case TAB_SYNC     -> syncTab     != null && syncTab.mouseClicked(mouseX, mouseY, button);
            case TAB_SETTINGS -> settingsTab != null && settingsTab.mouseClicked(mouseX, mouseY, button);
            default           -> false;
        };
        return handled || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        ModConfig.getInstance().save();
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause in multiplayer
    }
}
