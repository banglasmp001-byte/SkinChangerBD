package com.ahmad.skinchangebd.gui;

import com.ahmad.skinchangebd.SkinChangerBD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * A simple file-path input dialog for importing skin or cape PNGs.
 *
 * On desktop platforms the user types (or pastes) a full path.
 * On PojavLauncher/Android, the user can paste a path from a file manager.
 *
 * The screen calls the {@code callback} with the resolved Path on confirm,
 * or null on cancel/error.
 *
 * Created by Ahmad
 */
public class ImportFileScreen extends Screen {

    private final Screen         parent;
    private final boolean        isSkin;   // true = skin, false = cape
    private final Consumer<Path> callback;

    private TextFieldWidget pathField;
    private ButtonWidget    btnConfirm;
    private ButtonWidget    btnCancel;

    private String errorMessage = "";

    // Folder hint shown to the user
    private final String folderHint;

    public ImportFileScreen(Screen parent, boolean isSkin, Consumer<Path> callback) {
        super(Text.translatable(isSkin
                ? "skinchangebd.import.title_skin"
                : "skinchangebd.import.title_cape"));
        this.parent   = parent;
        this.isSkin   = isSkin;
        this.callback = callback;
        this.folderHint = (isSkin
                ? SkinChangerBD.SKIN_DIR
                : SkinChangerBD.CAPE_DIR).toString();
    }

    @Override
    protected void init() {
        int fw = Math.min(360, this.width - 40);
        int fx = (this.width - fw) / 2;
        int fy = this.height / 2 - 20;

        pathField = new TextFieldWidget(this.textRenderer, fx, fy, fw, 20,
                Text.translatable("skinchangebd.import.path_hint"));
        pathField.setMaxLength(512);
        pathField.setPlaceholder(Text.translatable("skinchangebd.import.path_placeholder"));
        addDrawableChild(pathField);

        int btnY = fy + 28;
        int bw   = 100;

        btnConfirm = addDrawableChild(ButtonWidget.builder(
                Text.translatable("skinchangebd.button.confirm"),
                b -> confirm())
                .dimensions(this.width / 2 - bw - 4, btnY, bw, 20).build());

        btnCancel = addDrawableChild(ButtonWidget.builder(
                Text.translatable("skinchangebd.button.cancel"),
                b -> cancel())
                .dimensions(this.width / 2 + 4, btnY, bw, 20).build());

        setInitialFocus(pathField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 56, 0xFFFFFF);

        // Instruction
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("skinchangebd.import.instruction"),
                this.width / 2, this.height / 2 - 44, 0xAAAAAA);

        // Folder hint
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("skinchangebd.import.or_drop_in").append(" " + folderHint),
                this.width / 2, this.height / 2 - 32, 0x888888);

        // Error message
        if (!errorMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(errorMessage), this.width / 2, this.height / 2 + 56, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER ||
            keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        String raw = pathField.getText().trim();
        if (raw.isBlank()) {
            errorMessage = "Please enter a file path.";
            return;
        }

        Path resolved;
        try {
            resolved = Paths.get(raw).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            errorMessage = "Invalid path: " + e.getReason();
            return;
        }

        if (!Files.exists(resolved)) {
            errorMessage = "File not found: " + resolved;
            return;
        }

        if (!resolved.getFileName().toString().toLowerCase().endsWith(".png")) {
            errorMessage = "Only PNG files are supported.";
            return;
        }

        // Security: must be a regular file, not a symlink to something dangerous
        if (!Files.isRegularFile(resolved)) {
            errorMessage = "Path is not a regular file.";
            return;
        }

        errorMessage = "";
        callback.accept(resolved);
    }

    private void cancel() {
        callback.accept(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
