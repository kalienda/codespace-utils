package me.vladosik.csu.mixin;

import me.vladosik.csu.CodespaceUtils;
import me.vladosik.csu.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.Hud.class)
public class HudMixin {
    private @Shadow @Final Minecraft minecraft;

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    void setOverlayMessage(Component string, boolean animate, CallbackInfo ci) {
        if (Utils.worldIsCodespace(minecraft.level) && CodespaceUtils.config.hideLineMemberHotbarMessage) {
            ci.cancel();
        }
    }
}
