package info.mudbourn.mmsdungeons.mixin.features;

import info.mudbourn.mmsdungeons.relations.DarkVision;
import info.mudbourn.mmsdungeons.relations.MobRelations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Forgives accidental hits between monsters and shortens what mobs see of targets standing in the dark.
@Mixin(LivingEntity.class)
public abstract class LivingEntityRelationsMixin {

    @Inject(method = "setLastHurtByMob", at = @At("HEAD"), cancellable = true)
    private void mmsDungeons$forgiveAccident(LivingEntity attacker, CallbackInfo ci) {
        if ((Object) this instanceof Mob victim && attacker != null && MobRelations.forgives(victim, attacker)) {
            ci.cancel();
        }
    }

    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void mmsDungeons$darkness(ServerLevel level, Entity looker, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValueD() * DarkVision.factor((LivingEntity) (Object) this));
    }
}
