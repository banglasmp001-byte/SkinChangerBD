package com.ahmad.skinchangebd.mixin;

import com.ahmad.skinchangebd.config.ModConfig;
import com.ahmad.skinchangebd.render.SkinTextureManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into PlayerEntityRenderer to substitute custom skin/cape textures
 * for players who are using SkinChangerBD.
 *
 * Created by Ahmad
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    /**
     * Override the skin texture returned for a player.
     * Returns the custom SkinChangerBD texture if one is available; falls back to default.
     */
    @Inject(
        method = "getSkin(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void skinchangebd_overrideSkin(
            AbstractClientPlayerEntity player,
            CallbackInfoReturnable<Identifier> cir) {

        if (!ModConfig.getInstance().isEnabled() || !ModConfig.getInstance().isSkinEnabled()) return;

        SkinTextureManager mgr = SkinTextureManager.getInstance();

        // Check if this is the local player
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getUuid().equals(player.getUuid())) {
            Identifier own = mgr.getOwnSkinIdentifier();
            if (own != null) cir.setReturnValue(own);
        } else {
            // Remote player
            String name = player.getNameForScoreboard();
            Identifier custom = mgr.getPlayerSkinIdentifier(name);
            if (custom != null) cir.setReturnValue(custom);
        }
    }
}
