package info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Finds places an entity can stand and moves it there with the Enderman teleport sound.
public final class NethermanSpots {

    private static final int RING_TRIES = 32;
    private static final int RING_DROP = 10;
    private static final int RING_RISE = 3;

    private NethermanSpots() {
    }

    // The first standable spot at or below a height, scanning down by up to the depth.
    public static Vec3 standingSpot(Level level, Entity entity, double x, double fromY, double z, int depth) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(fromY), Math.floor(z));
        for (int i = 0; i < depth; i++) {
            BlockPos below = pos.below();
            if (
                !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                    && level.getFluidState(below).isEmpty()
                    && level.getFluidState(pos).isEmpty()
                    && level.noCollision(entity, entity.getDimensions(entity.getPose()).makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))
            ) {
                return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            }
            pos.move(Direction.DOWN);
        }
        return null;
    }

    // A standable spot on a ring around the center, or null when none turns up.
    public static Vec3 ringSpot(Level level, Entity entity, Entity center, double minRange, double maxRange) {
        RandomSource random = entity.getRandom();
        for (int i = 0; i < RING_TRIES; i++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double range = minRange + random.nextDouble() * (maxRange - minRange);
            Vec3 spot = standingSpot(
                level,
                entity,
                center.getX() + Math.cos(angle) * range,
                center.getY() + RING_RISE,
                center.getZ() + Math.sin(angle) * range,
                RING_DROP
            );
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    // Moves the entity with a teleport sound at both ends.
    public static void teleport(Entity entity, Vec3 spot) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
        entity.teleportTo(spot.x, spot.y, spot.z);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
    }
}
