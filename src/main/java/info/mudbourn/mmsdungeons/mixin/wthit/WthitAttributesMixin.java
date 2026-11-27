package info.mudbourn.mmsdungeons.mixin.wthit;

import info.mudbourn.mmsdungeons.client.compat.wthit.MimicDisguisePlugin;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Keeps WTHIT from listing a closed Mimic's health, armor and effects, which no chest has.
@Pseudo
@Mixin(targets = {
    "mcp.mobius.waila.plugin.vanilla.provider.EntityAttributesProvider",
    "mcp.mobius.waila.plugin.vanilla.provider.MobEffectProvider"
}, remap = false)
public abstract class WthitAttributesMixin {

    @Inject(method = {"appendHead", "appendBody"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void mmsDungeons$hideMimic(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config, CallbackInfo ci) {
        if (MimicDisguisePlugin.disguised(accessor.getEntity())) {
            ci.cancel();
        }
    }
}
