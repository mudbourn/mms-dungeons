package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.client.CorpsePose;
import info.mudbourn.mmsdungeons.corpse.CorpseMarkers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Stamps a corpse marker's synced facing onto its render state, whatever pose the entity reports.
@Mixin(LivingEntityRenderer.class)
public abstract class CorpseStateMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL"))
    private void mmsDungeons$corpseState(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        state.setData(CorpsePose.FACING, entity.getAttachedOrElse(CorpseMarkers.FACING, Float.NaN));
    }
}
