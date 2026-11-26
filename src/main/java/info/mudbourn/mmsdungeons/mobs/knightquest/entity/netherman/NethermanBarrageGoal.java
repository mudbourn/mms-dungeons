package info.mudbourn.mmsdungeons.mobs.knightquest.entity.netherman;

import info.mudbourn.mmsdungeons.mobs.knightquest.KnightQuestMobs;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.Netherman;
import info.mudbourn.mmsdungeons.mobs.knightquest.entity.NethermanCharge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

// Ten quick teleports around the target, each leaving lava underfoot or a projectile charge overhead.
public class NethermanBarrageGoal extends Goal {

    public enum Kind {
        LAVA(1),
        CHARGES(3);

        private final int phase;

        Kind(int phase) {
            this.phase = phase;
        }
    }

    private static final int PERIOD = 300;
    private static final int BARRAGE_TICKS = 20;
    private static final double RANGE_SQUARED = 4096.0;
    private static final int TRIES = 50;
    private static final double MIN_RANGE = 5.0;
    private static final double MAX_RANGE = 20.0;
    private static final int MAX_CHARGE_HEIGHT = 10;

    private final Netherman netherman;
    private final Kind kind;
    private int chargeTime;

    public NethermanBarrageGoal(Netherman netherman, Kind kind) {
        this.netherman = netherman;
        this.kind = kind;
    }

    @Override
    public boolean canUse() {
        return this.netherman.getTarget() != null && this.netherman.isFormReady(this.kind.phase);
    }

    @Override
    public void start() {
        this.chargeTime = PERIOD;
    }

    @Override
    public void stop() {
        this.chargeTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.netherman.getTarget();
        if (target == null || this.netherman.distanceToSqr(target) >= RANGE_SQUARED || !this.netherman.hasLineOfSight(target)) {
            this.chargeTime = PERIOD;
            return;
        }
        if (this.chargeTime > 0) {
            this.chargeTime--;
        }
        if (this.chargeTime < BARRAGE_TICKS && this.chargeTime % 2 == 0 && this.netherman.level() instanceof ServerLevel level) {
            this.hop(level, target);
        }
        if (this.chargeTime == 0) {
            this.chargeTime = PERIOD;
        }
    }

    private void hop(ServerLevel level, LivingEntity target) {
        Vec3 best = null;
        RandomSource random = this.netherman.getRandom();
        for (int i = 0; i < TRIES; i++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double range = MIN_RANGE + random.nextDouble() * (MAX_RANGE - MIN_RANGE);
            Vec3 spot = NethermanSpots.standingSpot(
                level,
                this.netherman,
                target.getX() + Math.cos(angle) * range,
                target.getY() + 2.0,
                target.getZ() + Math.sin(angle) * range,
                6
            );
            if (spot != null && (best == null || this.netherman.position().distanceToSqr(spot) < this.netherman.position().distanceToSqr(best))) {
                best = spot;
            }
        }
        if (best == null) {
            return;
        }
        BlockPos pos = BlockPos.containing(best);
        if (this.kind == Kind.LAVA) {
            this.lay(level, pos.below());
        } else {
            this.drop(level, pos);
        }
        level.sendParticles(ParticleTypes.FLAME, this.netherman.getX(), this.netherman.getEyeY(), this.netherman.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
        NethermanSpots.teleport(this.netherman, best);
    }

    private void lay(ServerLevel level, BlockPos floor) {
        this.netherman.placeTempLava(level, floor);
    }

    private void drop(ServerLevel level, BlockPos pos) {
        NethermanCharge charge = KnightQuestMobs.NETHERMAN_CHARGE.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (charge == null) {
            return;
        }
        BlockPos at = pos.above(this.netherman.getRandom().nextIntBetweenInclusive(1, MAX_CHARGE_HEIGHT - 1));
        charge.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(charge);
    }
}
