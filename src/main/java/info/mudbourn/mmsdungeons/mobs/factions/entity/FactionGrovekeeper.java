package info.mudbourn.mmsdungeons.mobs.factions.entity;

import info.mudbourn.mmsorigins.entity.FloranHenchman;
import info.mudbourn.mmsorigins.power.action.SummonHenchmenAction;
import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

// Piloting a Grovekeeper the way a floran player does: merged into it, steering it, striking through it and sheltered by it. Only loaded with MMS Origins.
final class FactionGrovekeeper {

    private FactionGrovekeeper() {
    }

    static boolean carry(Entity keeper, FactionMember rider) {
        return keeper instanceof FloranHenchman golem && golem.isOwnedBy(rider.getUUID()) && golem.carry(rider);
    }

    static boolean roomFor(FactionMember owner, ServerLevel level) {
        return SummonHenchmenAction.roomFor(owner, level);
    }

    static int active(FactionMember owner, ServerLevel level) {
        return SummonHenchmenAction.active(owner, level).size();
    }

    static Entity raise(FactionMember owner, ServerLevel level) {
        return SummonHenchmenAction.raise(owner, level);
    }

    // The nearest of its own Grovekeepers with nobody riding it.
    static @Nullable Entity freeKeeper(FactionMember owner, ServerLevel level, double range) {
        return SummonHenchmenAction.active(owner, level)
            .stream()
            .filter(golem -> !golem.isVehicle() && owner.distanceTo(golem) < range)
            .min(Comparator.comparingDouble(owner::distanceToSqr))
            .orElse(null);
    }

    static boolean piloting(FactionMember rider) {
        return rider.getVehicle() instanceof FloranHenchman;
    }

    static void strike(FactionMember rider, LivingEntity target) {
        if (rider.getVehicle() instanceof FloranHenchman golem) {
            golem.getLookControl().setLookAt(target);
            golem.strikeFor(rider, target);
        }
    }

    // A mob's blow at the rider lands on the golem instead.
    static boolean shields(FactionMember rider, DamageSource source) {
        return rider.getVehicle() instanceof FloranHenchman && source.getEntity() instanceof Mob;
    }

    static boolean takeHit(ServerLevel level, FactionMember rider, DamageSource source, float amount) {
        return rider.getVehicle() instanceof FloranHenchman golem && golem.hurtServer(level, source, amount);
    }
}
