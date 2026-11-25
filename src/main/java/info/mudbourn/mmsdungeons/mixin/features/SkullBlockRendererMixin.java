package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.client.mobs.kobolds.KoboldSkullModel;
import info.mudbourn.mmsdungeons.client.mobs.kobolds.KoboldsClient;
import info.mudbourn.mmsdungeons.mobs.kobolds.block.KoboldSkullType;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SkullBlock;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Gives the kobold skulls their own model and texture in the block renderer.
@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {

    @Inject(method = "createModel", at = @At("HEAD"), cancellable = true)
    private static void mmsDungeons$koboldModel(
        EntityModelSet modelSet,
        SkullBlock.Type type,
        CallbackInfoReturnable<SkullModelBase> cir
    ) {
        if (type instanceof KoboldSkullType) {
            cir.setReturnValue(new KoboldSkullModel(modelSet.bakeLayer(KoboldsClient.SKULL)));
        }
    }

    @Inject(method = "getSkullRenderType", at = @At("HEAD"), cancellable = true)
    private static void mmsDungeons$koboldRenderType(
        SkullBlock.Type type,
        @Nullable Identifier texture,
        CallbackInfoReturnable<RenderType> cir
    ) {
        if (type instanceof KoboldSkullType kobold) {
            cir.setReturnValue(RenderTypes.entityCutoutZOffset(KoboldsClient.skullTexture(kobold)));
        }
    }
}
