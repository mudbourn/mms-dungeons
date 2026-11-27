package info.mudbourn.mmsdungeons.mobs.fangsnclaws.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Velocity steering shared by the Fangs 'n Claws mobs that move without pathfinding.
public final class Steering {

    private static final double ARRIVE = 0.2;
    private static final double WATER_SLOWDOWN = 0.4;

    private Steering() {
    }

    // Blends a mob's flat velocity toward a point.
    public static void walkToward(Mob mob, Vec3 target, double speed, double blend) {
        double scaled = mob.isInWater() ? speed * WATER_SLOWDOWN : speed;
        double dx = target.x - mob.getX();
        double dz = target.z - mob.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= ARRIVE) {
            return;
        }
        Vec3 current = mob.getDeltaMovement();
        double wantedX = dx / distance * scaled;
        double wantedZ = dz / distance * scaled;
        double x = current.x + (wantedX - current.x) * blend;
        double z = current.z + (wantedZ - current.z) * blend;
        mob.setDeltaMovement(x, current.y, z);
    }

    // Blends a mob's full velocity toward a point.
    public static void flyToward(Mob mob, Vec3 target, double speed, double blend) {
        if (target == null) {
            return;
        }
        Vec3 offset = target.subtract(mob.position());
        if (offset.length() > ARRIVE) {
            mob.setDeltaMovement(mob.getDeltaMovement().lerp(offset.normalize().scale(speed), blend));
        }
    }

    // Turns a mob to face a point on the flat plane.
    public static void faceToward(Mob mob, Vec3 target) {
        double dx = target.x - mob.getX();
        double dz = target.z - mob.getZ();
        if (dx * dx + dz * dz < 0.01) {
            return;
        }
        float yaw = (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI);
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
    }

    // Turns a mob to face a point, nose up or down included.
    public static void faceTowardWithPitch(Mob mob, Vec3 target) {
        double dx = target.x - mob.getX();
        double dy = target.y - (mob.getY() + mob.getBbHeight() * 0.5);
        double dz = target.z - mob.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat * flat + dy * dy < 0.01) {
            return;
        }
        float yaw = (float) (Mth.atan2(-dx, dz) * 180.0 / Math.PI);
        float pitch = (float) (-Mth.atan2(dy, flat) * 180.0 / Math.PI);
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.setXRot(pitch);
        mob.xRotO = pitch;
    }

    // The top of the first solid block under a mob, within some blocks.
    public static double groundY(Level level, Mob mob, int depth, double fallback) {
        BlockPos base = mob.blockPosition();
        for (int i = 0; i <= depth; i++) {
            BlockPos check = base.below(i);
            if (!level.getBlockState(check).isAir()) {
                return check.getY() + 1.0;
            }
        }
        return fallback;
    }

    // A random point near a mob, or null when it sits in solid rock.
    public static Vec3 roamPoint(Level level, Mob mob, double range) {
        double x = mob.getX() + (mob.getRandom().nextDouble() * 2.0 - 1.0) * range;
        double y = mob.getY() + (mob.getRandom().nextDouble() * 2.0 - 1.0) * range * 0.5;
        double z = mob.getZ() + (mob.getRandom().nextDouble() * 2.0 - 1.0) * range;
        double clamped = Mth.clamp(y, level.getMinY() + 2, level.getMaxY() - 2);
        return level.getBlockState(BlockPos.containing(x, clamped, z)).isSolid() ? null : new Vec3(x, clamped, z);
    }
}
