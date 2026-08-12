package com.ahmad.skinchangebd.mixin;

import com.ahmad.skinchangebd.config.ModConfig;
import com.ahmad.skinchangebd.render.SkinTextureManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(
        method = "getSkinTextures",
        at = @At("RETURN"),
        cancellable = true
    )
    private void skinchangebd_overrideSkin(
            AbstractClientPlayerEntity player,
            CallbackInfoReturnable<SkinTextures> cir) {

        if (!ModConfig.getInstance().isEnabled() || !ModConfig.getInstance().isSkinEnabled()) return;

        SkinTextureManager mgr = SkinTextureManager.getInstance();
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

        Identifier customSkin = null;

        if (mc.player != null && mc.player.getUuid().equals(player.getUuid())) {
            customSkin = mgr.getOwnSkinIdentifier();
        } else {
            String name = player.getNameForScoreboard();
            customSkin = mgr.getPlayerSkinIdentifier(name);
        }

        if (customSkin != null) {
            SkinTextures original = cir.getReturnValue();
            SkinTextures modified = new SkinTextures(
                customSkin,
                original.textureUrl(),
                original.capeTexture(),
                original.elytraTexture(),
                original.model(),
                original.secure()
            );
            cir.setReturnValue(modified);
        }
    }
}
