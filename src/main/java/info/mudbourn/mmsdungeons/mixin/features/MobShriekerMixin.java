package info.mudbourn.mmsdungeons.mixin.features;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Lets a mob set off a shrieker, by vibration or by stepping on it. The shriek is pinned on the nearest player
// in warning range, so it raises their warning level and can summon a warden the same as if they had caused it.
@Mixin(SculkShriekerBlockEntity.class)
public abstract class MobShriekerMixin {

    // The vanilla shrieker warns players within this many blocks.
    @Unique
    private static final double WARNING_RANGE = 16.0;

    @Inject(method = "tryGetPlayer", at = @At("RETURN"), cancellable = true)
    private static void mmsDungeons$mobsShriek(Entity entity, CallbackInfoReturnable<ServerPlayer> cir) {
        if (cir.getReturnValue() != null || !(entity instanceof Mob) || entity instanceof Warden || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer nearest = null;
        double best = WARNING_RANGE * WARNING_RANGE;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(entity);
            if (distance <= best && !player.isSpectator() && !player.isCreative()) {
                best = distance;
                nearest = player;
            }
        }
        cir.setReturnValue(nearest);
    }
}
