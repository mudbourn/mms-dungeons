package info.mudbourn.mmsdungeons.mixin.features;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Gives a corpse mannequin a flat hitbox over its whole lying body instead of the tiny sleeping one.
@Mixin(LivingEntity.class)
public abstract class CorpseHitboxMixin {

    private static final EntityDimensions CORPSE = EntityDimensions.scalable(1.6F, 0.4F);
    private static final String CORPSE_DESCRIPTION = "Corpse";

    private static boolean mmsDungeons$isCorpse(Entity entity) {
        if (!(entity instanceof Mannequin) || entity.getPose() != Pose.SLEEPING) {
            return false;
        }
        Optional<Component> description = entity.getEntityData().get(MannequinAccessor.mmsDungeons$descriptionData());
        return description.isPresent() && CORPSE_DESCRIPTION.equals(description.get().getString());
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$corpseDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (mmsDungeons$isCorpse(self)) {
            cir.setReturnValue(CORPSE.scale(self.getScale()));
        }
    }

    @Inject(method = "onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V", at = @At("TAIL"))
    private void mmsDungeons$refreshCorpse(EntityDataAccessor<?> data, CallbackInfo ci) {
        if (data == MannequinAccessor.mmsDungeons$descriptionData() && mmsDungeons$isCorpse((Entity) (Object) this)) {
            ((Entity) (Object) this).refreshDimensions();
        }
    }
}
