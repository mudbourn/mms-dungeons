package info.mudbourn.mmsdungeons.mixin.features;

import com.mojang.serialization.MapCodec;
import info.mudbourn.mmsdungeons.client.mobs.fangsnclaws.PitchforkSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Registers the mms_dungeons:pitchfork special item model next to vanilla's.
@Mixin(SpecialModelRenderers.class)
public abstract class PitchforkSpecialModelMixin {

    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void mmsDungeons$registerPitchfork(CallbackInfo ci) {
        ID_MAPPER.put(PitchforkSpecialRenderer.TYPE, PitchforkSpecialRenderer.Unbaked.MAP_CODEC);
    }
}
