package com.ahmad.skinchangebd.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all SkinChangerBD tab sub-screens.
 * Manages visibility and delegates rendering/input to child widgets.
 *
 * Created by Ahmad
 */
public abstract class AbstractTabScreen {

    protected final Screen parent;
    protected final int x, y, width, height;

    protected MinecraftClient client;
    protected boolean visible = false;

    protected final List<ButtonWidget> buttons = new ArrayList<>();

    public AbstractTabScreen(Screen parent, int x, int y, int width, int height) {
        this.parent = parent;
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    public void init(MinecraftClient client, int screenWidth, int screenHeight) {
        this.client = client;
        buttons.clear();
        initWidgets();
    }

    protected abstract void initWidgets();

    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        for (ButtonWidget btn : buttons) {
            if (btn.isMouseOver(mouseX, mouseY)) {
                btn.onClick(mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    protected ButtonWidget addButton(int x, int y, int w, int h, Text label, ButtonWidget.PressAction action) {
        ButtonWidget btn = ButtonWidget.builder(label, action).dimensions(x, y, w, h).build();
        buttons.add(btn);
        return btn;
    }
}
